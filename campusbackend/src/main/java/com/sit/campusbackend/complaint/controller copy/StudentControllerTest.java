package com.sit.campusbackend.complaint.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sit.campusbackend.complaint.controller.student.StudentController;
import com.sit.campusbackend.complaint.dto.ComplaintRequest;
import com.sit.campusbackend.complaint.dto.ComplaintResponse;
import com.sit.campusbackend.complaint.entity.ComplaintPriority;
import com.sit.campusbackend.complaint.entity.ComplaintStatus;
import com.sit.campusbackend.complaint.service.ComplaintService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;
    @MockBean  ComplaintService complaintService;

    private ComplaintResponse sampleResponse() {
        ComplaintResponse r = new ComplaintResponse();
        r.setId(1L);
        r.setTitle("WiFi down");
        r.setDescription("The wifi is not working in the lab.");
        r.setCategory("IT");
        r.setStatus(ComplaintStatus.ASSIGNED);
        r.setPriority(ComplaintPriority.HIGH);
        r.setStudentEmail("john.doe.2023@sitpune.edu.in");
        r.setStudentName("john doe");
        r.setDepartmentName("IT Department");
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    // ── POST /student/complaint ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /student/complaint → 201 Created with response body and priority")
    void submitComplaint_validBody_returns201() throws Exception {
        ComplaintRequest req = new ComplaintRequest();
        req.setEmail("john.doe.2023@sitpune.edu.in");
        req.setTitle("WiFi down");
        req.setDescription("The wifi is not working in the lab.");
        req.setImageUrl("https://img.example.com/1.jpg");
        req.setPriority(ComplaintPriority.HIGH);

        when(complaintService.createComplaint(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/student/complaint")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.category").value("IT"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.departmentName").value("IT Department"));
    }

    @Test
    @DisplayName("POST /student/complaint → 400 when title is too short (@Valid)")
    void submitComplaint_shortTitle_returns400() throws Exception {
        mockMvc.perform(post("/student/complaint")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"john@sitpune.edu.in\",\"title\":\"Hi\"," +
                         "\"description\":\"The wifi is not working in the lab.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("title")));
    }

    @Test
    @DisplayName("POST /student/complaint → 400 when email is invalid (@Valid)")
    void submitComplaint_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/student/complaint")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"title\":\"Valid title here\"," +
                         "\"description\":\"The wifi is not working in the lab.\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /student/complaint without priority → 201, priority defaults to MEDIUM")
    void submitComplaint_noPriority_returns201WithMediumDefault() throws Exception {
        ComplaintResponse mediumResp = sampleResponse();
        mediumResp.setPriority(ComplaintPriority.MEDIUM);

        when(complaintService.createComplaint(any())).thenReturn(mediumResp);

        ComplaintRequest req = new ComplaintRequest();
        req.setEmail("john.doe.2023@sitpune.edu.in");
        req.setTitle("WiFi issue in lab");
        req.setDescription("The wifi is not working in the lab.");

        mockMvc.perform(post("/student/complaint")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    // ── GET /student/complaints/{email} ───────────────────────────────────────

    @Test
    @DisplayName("GET /student/complaints/{email} → 200 with list")
    void getMyComplaints_validEmail_returns200() throws Exception {
        when(complaintService.getStudentComplaints("john.doe.2023@sitpune.edu.in"))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/student/complaints/john.doe.2023@sitpune.edu.in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("WiFi down"));
    }

    @Test
    @DisplayName("GET /student/complaints/{email} → 200 empty list when no complaints")
    void getMyComplaints_noComplaints_returnsEmptyList() throws Exception {
        when(complaintService.getStudentComplaints(any())).thenReturn(List.of());

        mockMvc.perform(get("/student/complaints/nobody@sitpune.edu.in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
