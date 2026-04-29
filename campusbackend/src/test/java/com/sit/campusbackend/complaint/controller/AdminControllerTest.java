package com.sit.campusbackend.complaint.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sit.campusbackend.complaint.controller.admin.AdminController;
import com.sit.campusbackend.complaint.dto.ComplaintResponse;
import com.sit.campusbackend.complaint.dto.DashboardStatsResponse;
import com.sit.campusbackend.complaint.dto.StatusUpdateRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;
    @MockBean  ComplaintService complaintService;

    private ComplaintResponse buildResponse(Long id, ComplaintStatus status) {
        ComplaintResponse r = new ComplaintResponse();
        r.setId(id);
        r.setTitle("Test Complaint");
        r.setCategory("IT");
        r.setStatus(status);
        r.setPriority(ComplaintPriority.MEDIUM);
        r.setStudentEmail("student@sitpune.edu.in");
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    // ── GET /admin/complaints (paginated) ─────────────────────────────────────

    @Test
    @DisplayName("GET /admin/complaints → 200 with paginated result (default page=0, size=20)")
    void getAllComplaints_defaultPagination_returns200() throws Exception {
        List<ComplaintResponse> content = List.of(
                buildResponse(1L, ComplaintStatus.ASSIGNED),
                buildResponse(2L, ComplaintStatus.IN_PROGRESS));

        Page<ComplaintResponse> page = new PageImpl<>(content, PageRequest.of(0, 20), 2);

        when(complaintService.getAllComplaints(any())).thenReturn(page);

        mockMvc.perform(get("/admin/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].status").value("ASSIGNED"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /admin/complaints?page=1&size=1 → 200 with second page")
    void getAllComplaints_customPagination_returnsCorrectPage() throws Exception {
        List<ComplaintResponse> secondPage = List.of(buildResponse(2L, ComplaintStatus.IN_PROGRESS));
        Page<ComplaintResponse> page = new PageImpl<>(secondPage, PageRequest.of(1, 1), 2);

        when(complaintService.getAllComplaints(any())).thenReturn(page);

        mockMvc.perform(get("/admin/complaints").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    @DisplayName("GET /admin/complaints → 200 with empty page when no complaints")
    void getAllComplaints_empty_returnsEmptyPage() throws Exception {
        Page<ComplaintResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(complaintService.getAllComplaints(any())).thenReturn(emptyPage);

        mockMvc.perform(get("/admin/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── GET /admin/stats ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /admin/stats → 200 with dashboard counts")
    void getStats_returns200WithCounts() throws Exception {
        DashboardStatsResponse stats = new DashboardStatsResponse(10, 2, 3, 2, 2, 1);
        when(complaintService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.pending").value(2))
                .andExpect(jsonPath("$.assigned").value(3))
                .andExpect(jsonPath("$.inProgress").value(2))
                .andExpect(jsonPath("$.resolved").value(2))
                .andExpect(jsonPath("$.closed").value(1));
    }

    // ── PUT /admin/status ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /admin/status → 200 with updated complaint")
    void updateStatus_validRequest_returns200() throws Exception {
        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setComplaintId(1L);
        req.setStatus(ComplaintStatus.IN_PROGRESS);

        when(complaintService.updateStatus(1L, ComplaintStatus.IN_PROGRESS))
                .thenReturn(buildResponse(1L, ComplaintStatus.IN_PROGRESS));

        mockMvc.perform(put("/admin/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("PUT /admin/status → 400 when complaintId is null")
    void updateStatus_missingComplaintId_returns400() throws Exception {
        // status field present but complaintId missing
        mockMvc.perform(put("/admin/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /admin/status → 404 when complaint not found")
    void updateStatus_notFound_returns404() throws Exception {
        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setComplaintId(999L);
        req.setStatus(ComplaintStatus.CLOSED);

        when(complaintService.updateStatus(eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Complaint not found: 999"));

        mockMvc.perform(put("/admin/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Complaint not found: 999"));
    }
}
