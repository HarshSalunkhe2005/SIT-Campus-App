package com.sit.campusbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.sit.campusbackend.complaint.entity.Complaint;
import com.sit.campusbackend.complaint.entity.ComplaintStatus;
import com.sit.campusbackend.complaint.repository.ComplaintRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The progress history shown as a timeline in the UI. */
class HistoryIntegrationTest extends IntegrationTestBase {

    @Autowired EntityManagerFactory emf;
    @Autowired JdbcTemplate jdbc;
    @Autowired ComplaintRepository complaintRepo;

    private String studentToken() throws Exception {
        String email = uniqueEmail();
        student(email, STRONG_PASSWORD, true);
        return login(email, STRONG_PASSWORD);
    }

    private long submit(String token) throws Exception {
        String body = mvc.perform(report(token, "Hall", "fan broken", "ELECTRICAL", png()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private List<String> statuses(JsonNode history) {
        List<String> out = new ArrayList<>();
        history.forEach(e -> out.add(e.get("status").asText()));
        return out;
    }

    private void setStatus(String token, long id, String status) throws Exception {
        mvc.perform(put("/dept/status").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("complaintId", id, "status", status)))).andExpect(status().isOk());
    }

    @Test
    void historyIsRecordedFromCreationThroughResolution() throws Exception {
        dept("Electrical", "Dept#1234");
        dept("General", "Dept#1234");
        String student = studentToken();
        String deptToken = login("electrical@sitpune.edu.in", "Dept#1234");

        String created = mvc.perform(report(student, "Hall", "fan broken", "ELECTRICAL", png()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.history", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        assertEquals(List.of("ASSIGNED"), statuses(json.readTree(created).get("history")));

        setStatus(deptToken, id, "IN_PROGRESS");
        setStatus(deptToken, id, "IN_PROGRESS"); // no change, no new step
        setStatus(deptToken, id, "RESOLVED");

        String mine = mvc.perform(get("/student/my-reports").header("Authorization", "Bearer " + student))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode history = json.readTree(mine).get(0).get("history");
        assertEquals(List.of("ASSIGNED", "IN_PROGRESS", "RESOLVED"), statuses(history));
        for (int i = 1; i < history.size(); i++) {
            assertTrue(history.get(i).get("at").asText().compareTo(history.get(i - 1).get("at").asText()) >= 0, "oldest first");
        }

        // the department board and the admin list carry it too; the public feed does not
        mvc.perform(get("/dept/queue/" + departments.findByType("Electrical").orElseThrow().getId())
                        .header("Authorization", "Bearer " + deptToken))
                .andExpect(jsonPath("$[0].history", hasSize(3)));
        mvc.perform(get("/student/all-reports").header("Authorization", "Bearer " + student))
                .andExpect(jsonPath("$[0].history", hasSize(0)));
    }

    @Test
    void adminStatusChangesAreRecordedToo() throws Exception {
        admin("admin@sitpune.edu.in", "Adm1n#Pass");
        dept("General", "Dept#1234");
        String student = studentToken();
        long id = submit(student);
        String adminToken = login("admin@sitpune.edu.in", "Adm1n#Pass");

        mvc.perform(put("/admin/status").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("complaintId", id, "status", "CLOSED")))).andExpect(status().isOk());
        mvc.perform(get("/admin/all-complaints").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$[0].history", hasSize(2)))
                .andExpect(jsonPath("$[0].history[1].status").value("CLOSED"));
    }

    @Test
    void complaintsFromBeforeHistoryExistedStillGetAFullTimeline() throws Exception {
        dept("General", "Dept#1234");
        dept("Electrical", "Dept#1234");
        String studentEmail = uniqueEmail();
        student(studentEmail, STRONG_PASSWORD, true);
        String student = login(studentEmail, STRONG_PASSWORD);
        long id = submit(student);
        jdbc.update("delete from complaint_events"); // what an old row looks like: no history at all
        jdbc.update("update complaints set status = 'RESOLVED' where id = ?", id);

        String deptToken = login("electrical@sitpune.edu.in", "Dept#1234");
        mvc.perform(get("/student/my-reports").header("Authorization", "Bearer " + student))
                .andExpect(jsonPath("$[0].history", hasSize(2)))
                .andExpect(jsonPath("$[0].history[0].status").value("ASSIGNED"))
                .andExpect(jsonPath("$[0].history[1].status").value("RESOLVED"));

        // and the next real change builds on that implied first step instead of losing it
        setStatus(deptToken, id, "IN_PROGRESS");
        jdbc.update("update complaints set status = 'ASSIGNED' where id = ?", id);
        jdbc.update("delete from complaint_events");
        setStatus(deptToken, id, "IN_PROGRESS");
        mvc.perform(get("/student/my-reports").header("Authorization", "Bearer " + student))
                .andExpect(jsonPath("$[0].history[0].status").value("ASSIGNED"))
                .andExpect(jsonPath("$[0].history[1].status").value("IN_PROGRESS"));
    }

    @Test
    void deletingComplaintsRemovesTheirHistory() throws Exception {
        admin("admin@sitpune.edu.in", "Adm1n#Pass");
        dept("General", "Dept#1234");
        String studentEmail = uniqueEmail();
        student(studentEmail, STRONG_PASSWORD, true);
        submit(login(studentEmail, STRONG_PASSWORD));
        assertEquals(1, jdbc.queryForObject("select count(*) from complaint_events", Integer.class));

        String adminToken = login("admin@sitpune.edu.in", "Adm1n#Pass");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/admin/user/" + studentEmail)
                .header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        assertEquals(0, jdbc.queryForObject("select count(*) from complaint_events", Integer.class));
    }

    @Test
    void historyForAListIsOneQueryNotOnePerComplaint() throws Exception {
        dept("General", "Dept#1234");
        String student = studentToken();
        for (int i = 0; i < 10; i++) submit(student);

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        mvc.perform(get("/student/my-reports").header("Authorization", "Bearer " + student))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[9].history", hasSize(1)));
        long queries = stats.getPrepareStatementCount();
        stats.setStatisticsEnabled(false);
        assertTrue(queries <= 4, "my-reports ran " + queries + " statements for 10 complaints");
    }
}
