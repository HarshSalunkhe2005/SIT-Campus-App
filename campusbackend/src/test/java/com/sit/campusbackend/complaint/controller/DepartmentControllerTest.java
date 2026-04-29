package com.sit.campusbackend.complaint.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sit.campusbackend.complaint.controller.department.DepartmentController;
import com.sit.campusbackend.complaint.dto.ComplaintResponse;
import com.sit.campusbackend.complaint.dto.LoginRequest;
import com.sit.campusbackend.complaint.dto.LoginResponse;
import com.sit.campusbackend.complaint.dto.ResolveRequest;
import com.sit.campusbackend.complaint.entity.ComplaintPriority;
import com.sit.campusbackend.complaint.entity.ComplaintStatus;
import com.sit.campusbackend.complaint.exception.ResourceNotFoundException;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DepartmentControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;
    @MockBean  ComplaintService complaintService;

    private ComplaintResponse resolvedResponse() {
        ComplaintResponse r = new ComplaintResponse();
        r.setId(1L);
        r.setTitle("WiFi down");
        r.setCategory("IT");
        r.setStatus(ComplaintStatus.RESOLVED);
        r.setPriority(ComplaintPriority.MEDIUM);
        r.setResolvedImageUrl("https://img.example.com/proof.jpg");
        r.setStudentEmail("john.doe.2023@sitpune.edu.in");
        r.setDepartmentName("IT Department");
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    // ── POST /dept/login ──────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /dept/login → 200 with DEPARTMENT role and dept details")
    void deptLogin_validCredentials_returns200() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("it@sitpune.edu.in");
        req.setPassword("dept@123");

        LoginResponse resp = new LoginResponse("DEPARTMENT", 2L, "IT Department", null);
        when(complaintService.departmentLogin("it@sitpune.edu.in", "dept@123"))
                .thenReturn(resp);

        mockMvc.perform(post("/dept/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DEPARTMENT"))
                .andExpect(jsonPath("$.departmentId").value(2))
                .andExpect(jsonPath("$.departmentName").value("IT Department"));
    }

    @Test
    @DisplayName("POST /dept/login → 400 when wrong password")
    void deptLogin_wrongPassword_returns400() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("it@sitpune.edu.in");
        req.setPassword("wrongpass");

        when(complaintService.departmentLogin("it@sitpune.edu.in", "wrongpass"))
                .thenThrow(new IllegalArgumentException("Invalid email or password"));

        mockMvc.perform(post("/dept/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /dept/login → 404 when department email not found")
    void deptLogin_unknownEmail_returns404() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@sitpune.edu.in");
        req.setPassword("dept@123");

        when(complaintService.departmentLogin(anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException("No department found with email: ghost@sitpune.edu.in"));

        mockMvc.perform(post("/dept/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No department found with email: ghost@sitpune.edu.in"));
    }

    @Test
    @DisplayName("POST /dept/login → 400 when email field is blank (@Valid)")
    void deptLogin_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/dept/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"password\":\"dept@123\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /dept/complaints/{departmentId} ───────────────────────────────────

    @Test
    @DisplayName("GET /dept/complaints/{departmentId} → 200 with list")
    void getDeptComplaints_returns200() throws Exception {
        when(complaintService.getDepartmentComplaints(2L)).thenReturn(List.of(resolvedResponse()));

        mockMvc.perform(get("/dept/complaints/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].departmentName").value("IT Department"))
                .andExpect(jsonPath("$[0].priority").value("MEDIUM"));
    }

    // ── PUT /dept/resolve ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /dept/resolve → 200 with RESOLVED status")
    void resolveComplaint_valid_returns200() throws Exception {
        ResolveRequest req = new ResolveRequest();
        req.setComplaintId(1L);
        req.setResolvedImageUrl("https://img.example.com/proof.jpg");

        when(complaintService.resolveComplaint(eq(1L), anyString()))
                .thenReturn(resolvedResponse());

        mockMvc.perform(put("/dept/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedImageUrl").value("https://img.example.com/proof.jpg"));
    }

    @Test
    @DisplayName("PUT /dept/resolve → 400 when resolvedImageUrl is blank (@Valid)")
    void resolveComplaint_blankImageUrl_returns400() throws Exception {
        mockMvc.perform(put("/dept/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"complaintId\":1,\"resolvedImageUrl\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /dept/resolve → 404 when complaint does not exist")
    void resolveComplaint_notFound_returns404() throws Exception {
        ResolveRequest req = new ResolveRequest();
        req.setComplaintId(404L);
        req.setResolvedImageUrl("https://img.example.com/proof.jpg");

        when(complaintService.resolveComplaint(eq(404L), anyString()))
                .thenThrow(new ResourceNotFoundException("Complaint not found: 404"));

        mockMvc.perform(put("/dept/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Complaint not found: 404"));
    }
}
