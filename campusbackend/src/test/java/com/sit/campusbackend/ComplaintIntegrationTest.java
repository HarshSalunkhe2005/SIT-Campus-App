package com.sit.campusbackend;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mock.web.MockMultipartFile;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ComplaintIntegrationTest extends IntegrationTestBase {

    @Autowired EntityManagerFactory emf;

    private String studentToken(String email) throws Exception {
        student(email, STRONG_PASSWORD, true);
        return login(email, STRONG_PASSWORD);
    }

    private JsonNode submit(String token, String location, String description, String category) throws Exception {
        String body = mvc.perform(report(token, location, description, category, png()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    // ── reporting ────────────────────────────────────────────────────────────────────────────────

    @Test
    void reportIsRoutedToTheDepartmentForTheChosenCategory() throws Exception {
        dept("Electrical", "Dept#1234");
        dept("IT", "Dept#1234");
        dept("Cleaning", "Dept#1234");
        dept("General", "Dept#1234");
        String token = studentToken(uniqueEmail());

        // the form sends upper-case values; "IT" and "CLEANLINESS" used to fall through to General
        assertEquals("IT Department", submit(token, "Lab 2", "no connectivity", "IT").get("departmentName").asText());
        assertEquals("Cleaning Department", submit(token, "Block A", "dirty", "CLEANLINESS").get("departmentName").asText());
        assertEquals("Electrical Department", submit(token, "Room 1", "broken", "ELECTRICAL").get("departmentName").asText());
        assertEquals("General Department", submit(token, "Gate", "something", "OTHER").get("departmentName").asText());

        JsonNode it = submit(token, "Lab 2", "x", "IT");
        assertEquals("IT", it.get("category").asText());
        assertEquals("ASSIGNED", it.get("status").asText());
        assertEquals("Lab 2", it.get("location").asText());
    }

    @Test
    void withoutACategoryItIsDetectedFromTheDescriptionAndFallsBackToGeneral() throws Exception {
        dept("IT", "Dept#1234");
        dept("General", "Dept#1234");
        String token = studentToken(uniqueEmail());

        assertEquals("IT Department", submit(token, "Lab", "The wifi is down again", null).get("departmentName").asText());
        // "it" and "ac" inside other words must not trigger IT / Electrical
        assertEquals("General Department", submit(token, "Lab", "within the place, nothing works", null).get("departmentName").asText());
    }

    @Test
    void aCategoryWithNoDepartmentFallsBackToGeneral() throws Exception {
        dept("General", "Dept#1234");
        String token = studentToken(uniqueEmail());
        assertEquals("General Department", submit(token, "Hall", "chair broken", "FURNITURE").get("departmentName").asText());
    }

    @Test
    void reportNeedsAGeneralDepartmentToFallBackOn() throws Exception {
        String token = studentToken(uniqueEmail());
        mvc.perform(report(token, "Hall", "chair broken", "FURNITURE", png())).andExpect(status().isNotFound());
    }

    @Test
    void photoIsValidatedAndStored() throws Exception {
        dept("General", "Dept#1234");
        String token = studentToken(uniqueEmail());

        // missing, empty, wrong type, fake extension
        mvc.perform(report(token, "Hall", "broken", "OTHER", null)).andExpect(status().isBadRequest());
        mvc.perform(report(token, "Hall", "broken", "OTHER", new MockMultipartFile("image", "a.png", "image/png", new byte[0])))
                .andExpect(status().isBadRequest());
        mvc.perform(report(token, "Hall", "broken", "OTHER",
                        new MockMultipartFile("image", "a.png", "image/png", "<script>alert(1)</script>".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("JPEG, PNG or WebP")));

        // a real one is stored and then served publicly with an image content type
        JsonNode created = submit(token, "Hall", "broken", "OTHER");
        String url = created.get("imageUrl").asText();
        assertTrue(url.startsWith("/uploads/") && url.endsWith(".png"), url);
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG));
    }

    @Test
    void oversizedPhotoIsRejected() throws Exception {
        dept("General", "Dept#1234");
        String token = studentToken(uniqueEmail());
        byte[] big = new byte[6 * 1024 * 1024];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        mvc.perform(report(token, "Hall", "broken", "OTHER", new MockMultipartFile("image", "big.png", "image/png", big)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void textFieldsAreValidated() throws Exception {
        dept("General", "Dept#1234");
        String token = studentToken(uniqueEmail());
        mvc.perform(report(token, "", "broken", "OTHER", png())).andExpect(status().isBadRequest());
        mvc.perform(report(token, "Hall", " ", "OTHER", png())).andExpect(status().isBadRequest());
        mvc.perform(report(token, "Hall", "x".repeat(501), "OTHER", png())).andExpect(status().isBadRequest());
        mvc.perform(report(token, "y".repeat(101), "broken", "OTHER", png())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/student/report").file(png()).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest()); // missing the "complaint" part
    }

    @Test
    void aClientCannotSupplyItsOwnImageUrl() throws Exception {
        dept("General", "Dept#1234");
        String token = studentToken(uniqueEmail());
        String payload = "{\"location\":\"Hall\",\"description\":\"broken\",\"category\":\"OTHER\",\"imageUrl\":\"javascript:alert(1)\"}";
        String body = mvc.perform(multipart("/student/report")
                        .file(new MockMultipartFile("complaint", "", "application/json", payload.getBytes())).file(png())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(json.readTree(body).get("imageUrl").asText().startsWith("/uploads/"));
    }

    // ── visibility ───────────────────────────────────────────────────────────────────────────────

    @Test
    void myReportsOnlyShowsMyOwnAndTheFeedHidesEmailAddresses() throws Exception {
        dept("General", "Dept#1234");
        String aEmail = uniqueEmail(), bEmail = uniqueEmail();
        String a = studentToken(aEmail), b = studentToken(bEmail);
        submit(a, "A1", "one", "OTHER");
        submit(a, "A2", "two", "OTHER");
        submit(b, "B1", "three", "OTHER");

        mvc.perform(get("/student/my-reports").header("Authorization", "Bearer " + a))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].studentEmail").value(aEmail))
                .andExpect(jsonPath("$[0].location").value("A2")); // newest first

        mvc.perform(get("/student/all-reports").header("Authorization", "Bearer " + a))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].studentEmail").value(nullValue()))
                .andExpect(jsonPath("$[0].studentName").value("Test Student"));
    }

    @Test
    void listingComplaintsDoesNotRunOneQueryPerRow() throws Exception {
        dept("General", "Dept#1234");
        String token = studentToken(uniqueEmail());
        for (int i = 0; i < 12; i++) submit(token, "Loc " + i, "issue " + i, "OTHER");

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        mvc.perform(get("/student/all-reports").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(12)));
        long queries = stats.getPrepareStatementCount();
        stats.setStatisticsEnabled(false);

        assertTrue(queries <= 4, "feed ran " + queries + " statements for 12 complaints");
    }

    // ── upvotes ──────────────────────────────────────────────────────────────────────────────────

    @Test
    void eachStudentCanUpvoteAnIssueOnlyOnce() throws Exception {
        dept("General", "Dept#1234");
        String a = studentToken(uniqueEmail()), b = studentToken(uniqueEmail());
        long id = submit(a, "Hall", "broken", "OTHER").get("id").asLong();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/student/upvote/" + id)
                .header("Authorization", "Bearer " + a)).andExpect(status().isOk()).andExpect(jsonPath("$.upvoteCount").value(1));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/student/upvote/" + id)
                .header("Authorization", "Bearer " + a)).andExpect(status().isBadRequest());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/student/upvote/" + id)
                .header("Authorization", "Bearer " + b)).andExpect(status().isOk()).andExpect(jsonPath("$.upvoteCount").value(2));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/student/upvote/99999")
                .header("Authorization", "Bearer " + a)).andExpect(status().isNotFound());
    }

    @Test
    void anExistingUpvoteCountIsKeptWhenSomeoneNewUpvotes() throws Exception {
        dept("General", "Dept#1234");
        String a = studentToken(uniqueEmail());
        long id = submit(a, "Hall", "broken", "OTHER").get("id").asLong();
        var c = complaints.findById(id).orElseThrow();
        c.setUpvoteCount(7); // from before upvoters were tracked
        complaints.save(c);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/student/upvote/" + id)
                .header("Authorization", "Bearer " + a)).andExpect(jsonPath("$.upvoteCount").value(8));
    }

    // ── department ───────────────────────────────────────────────────────────────────────────────

    @Test
    void aDepartmentOnlySeesAndChangesItsOwnComplaints() throws Exception {
        var electrical = dept("Electrical", "Dept#1234");
        var it = dept("IT", "Dept#1234");
        dept("General", "Dept#1234");
        String student = studentToken(uniqueEmail());
        long electricalIssue = submit(student, "R1", "fan", "ELECTRICAL").get("id").asLong();
        long itIssue = submit(student, "R2", "wifi", "IT").get("id").asLong();

        String electricalToken = login("electrical@sitpune.edu.in", "Dept#1234");

        mvc.perform(get("/dept/queue/" + electrical.getId()).header("Authorization", "Bearer " + electricalToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(electricalIssue));
        mvc.perform(get("/dept/queue/" + it.getId()).header("Authorization", "Bearer " + electricalToken))
                .andExpect(status().isForbidden());

        // cannot touch another department's complaint, status or proof
        mvc.perform(put("/dept/status").header("Authorization", "Bearer " + electricalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("complaintId", itIssue, "status", "RESOLVED"))))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/dept/" + itIssue + "/proof").file(png()).header("Authorization", "Bearer " + electricalToken))
                .andExpect(status().isForbidden());
        assertEquals("ASSIGNED", complaints.findById(itIssue).orElseThrow().getStatus().name());
    }

    @Test
    void aDepartmentMovesItsComplaintForwardAndTheStudentIsToldOnceWhenResolved() throws Exception {
        dept("Electrical", "Dept#1234");
        dept("General", "Dept#1234");
        String studentEmail = uniqueEmail();
        String student = studentToken(studentEmail);
        long id = submit(student, "Room 9", "fan", "ELECTRICAL").get("id").asLong();
        String token = login("electrical@sitpune.edu.in", "Dept#1234");

        java.util.function.Function<String, org.springframework.test.web.servlet.ResultActions> setStatus = s -> {
            try {
                return mvc.perform(put("/dept/status").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("complaintId", id, "status", s))));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        setStatus.apply("in_progress").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        Mockito.verifyNoInteractions(mailSender);

        mvc.perform(multipart("/dept/" + id + "/proof").file(png()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resolvedImageUrl").value(containsString("/uploads/")));

        setStatus.apply("RESOLVED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
        setStatus.apply("RESOLVED").andExpect(status().isOk()); // repeating it must not email again
        org.mockito.ArgumentCaptor<SimpleMailMessage> mail = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender, Mockito.times(1)).send(mail.capture());
        assertEquals(studentEmail, mail.getValue().getTo()[0]);
        assertTrue(mail.getValue().getSubject().contains("Room 9"));

        // departments cannot close, and bad values are rejected
        setStatus.apply("CLOSED").andExpect(status().isForbidden());
        setStatus.apply("BOGUS").andExpect(status().isBadRequest());
    }

    @Test
    void aFailingResolutionEmailDoesNotUndoTheStatusChange() throws Exception {
        dept("Electrical", "Dept#1234");
        dept("General", "Dept#1234");
        String student = studentToken(uniqueEmail());
        long id = submit(student, "Room 9", "fan", "ELECTRICAL").get("id").asLong();
        String token = login("electrical@sitpune.edu.in", "Dept#1234");

        Mockito.doThrow(new org.springframework.mail.MailSendException("smtp down"))
                .when(mailSender).send(Mockito.any(SimpleMailMessage.class));
        mvc.perform(put("/dept/status").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("complaintId", id, "status", "RESOLVED"))))
                .andExpect(status().isOk());
        assertEquals("RESOLVED", complaints.findById(id).orElseThrow().getStatus().name());
    }

    // ── admin ────────────────────────────────────────────────────────────────────────────────────

    @Test
    void adminSeesEverythingAndCanCloseIssues() throws Exception {
        admin("admin@sitpune.edu.in", "Adm1n#Pass");
        dept("General", "Dept#1234");
        String student = studentToken(uniqueEmail());
        long id = submit(student, "Hall", "broken", "OTHER").get("id").asLong();
        submit(student, "Hall 2", "broken", "OTHER");
        String token = login("admin@sitpune.edu.in", "Adm1n#Pass");

        mvc.perform(get("/admin/all-complaints").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].studentEmail").isNotEmpty());
        mvc.perform(put("/admin/status").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("complaintId", id, "status", "CLOSED"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
        mvc.perform(get("/admin/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.assigned").value(1)).andExpect(jsonPath("$.closed").value(1))
                .andExpect(jsonPath("$.pending").value(0)).andExpect(jsonPath("$.resolved").value(0));
        mvc.perform(put("/admin/status").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void passwordHashesAreNeverReturned() throws Exception {
        admin("admin@sitpune.edu.in", "Adm1n#Pass");
        dept("General", "Dept#1234");
        student(uniqueEmail(), STRONG_PASSWORD, true);
        String token = login("admin@sitpune.edu.in", "Adm1n#Pass");

        String users = mvc.perform(get("/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].isVerified").value(true))
                .andReturn().getResponse().getContentAsString();
        String depts = mvc.perform(get("/admin/depts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (String body : new String[]{users, depts}) {
            assertTrue(!body.toLowerCase().contains("password") && !body.contains("$2a$"), body);
        }
    }

    @Test
    void adminCanCreateEditAndDeleteDepartments() throws Exception {
        admin("admin@sitpune.edu.in", "Adm1n#Pass");
        dept("General", "Dept#1234");
        String token = login("admin@sitpune.edu.in", "Adm1n#Pass");
        String bearer = "Bearer " + token;

        // generated password is returned once and works
        String created = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/dept")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Civil Dept", "type", "Civil", "email", "Civil@sitpune.edu.in"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.initialPassword").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        String generated = json.readTree(created).get("initialPassword").asText();
        postJson("/auth/login", Map.of("email", "civil@sitpune.edu.in", "password", generated)).andExpect(status().isOk());

        // duplicate category is a clean conflict, bad input a 400
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/dept")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Dup", "type", "Civil", "email", "dup@sitpune.edu.in"))))
                .andExpect(status().isConflict());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/dept")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "X", "type", "X", "email", "not-an-email"))))
                .andExpect(status().isBadRequest());

        // edit with a new password; the id, and the old password stops working
        mvc.perform(put("/admin/dept/" + id).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Civil Works", "type", "Civil",
                                "email", "civil@sitpune.edu.in", "password", "NewDept#99"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Civil Works"))
                .andExpect(jsonPath("$.initialPassword").doesNotExist());
        postJson("/auth/login", Map.of("email", "civil@sitpune.edu.in", "password", generated)).andExpect(status().isBadRequest());
        postJson("/auth/login", Map.of("email", "civil@sitpune.edu.in", "password", "NewDept#99")).andExpect(status().isOk());

        mvc.perform(delete("/admin/dept/" + id).header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(delete("/admin/dept/" + id).header("Authorization", bearer)).andExpect(status().isNotFound());
    }

    @Test
    void deletingADepartmentOrStudentRemovesTheirComplaints() throws Exception {
        admin("admin@sitpune.edu.in", "Adm1n#Pass");
        var electrical = dept("Electrical", "Dept#1234");
        dept("General", "Dept#1234");
        String sEmail = uniqueEmail();
        String student = studentToken(sEmail);
        submit(student, "R1", "fan", "ELECTRICAL");
        submit(student, "R2", "tap", "OTHER");
        String bearer = "Bearer " + login("admin@sitpune.edu.in", "Adm1n#Pass");

        mvc.perform(delete("/admin/dept/" + electrical.getId()).header("Authorization", bearer)).andExpect(status().isOk());
        assertEquals(1, complaints.count());
        mvc.perform(delete("/admin/user/" + sEmail).header("Authorization", bearer)).andExpect(status().isOk());
        assertEquals(0, complaints.count());
        assertTrue(students.findById(sEmail).isEmpty());
    }

    @Test
    void adminCanDisableAndReEnableAStudent() throws Exception {
        admin("admin@sitpune.edu.in", "Adm1n#Pass");
        String sEmail = uniqueEmail();
        student(sEmail, STRONG_PASSWORD, true);
        String bearer = "Bearer " + login("admin@sitpune.edu.in", "Adm1n#Pass");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/user/" + sEmail + "/toggle")
                .header("Authorization", bearer)).andExpect(status().isOk());
        assertTrue(!students.findById(sEmail).orElseThrow().getIsVerified());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/user/" + sEmail + "/toggle")
                .header("Authorization", bearer)).andExpect(status().isOk());
        assertTrue(students.findById(sEmail).orElseThrow().getIsVerified());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/user/nobody@sitpune.edu.in/toggle")
                .header("Authorization", bearer)).andExpect(status().isNotFound());
    }

    @Test
    void adminCanChangeTheirOwnPassword() throws Exception {
        admin("admin@sitpune.edu.in", "Adm1n#Pass");
        String token = login("admin@sitpune.edu.in", "Adm1n#Pass");

        postJson("/admin/password", Map.of("currentPassword", "wrong", "newPassword", "Brand#New99"), token)
                .andExpect(status().isBadRequest());
        postJson("/admin/password", Map.of("currentPassword", "Adm1n#Pass", "newPassword", "short"), token)
                .andExpect(status().isBadRequest());
        postJson("/admin/password", Map.of("currentPassword", "Adm1n#Pass", "newPassword", "Brand#New99"), token)
                .andExpect(status().isOk());

        postJson("/auth/login", Map.of("email", "admin@sitpune.edu.in", "password", "Adm1n#Pass")).andExpect(status().isBadRequest());
        postJson("/auth/login", Map.of("email", "admin@sitpune.edu.in", "password", "Brand#New99")).andExpect(status().isOk());
        assertNotNull(token);
    }
}
