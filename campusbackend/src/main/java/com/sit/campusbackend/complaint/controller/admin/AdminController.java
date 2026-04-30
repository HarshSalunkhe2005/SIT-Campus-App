package com.sit.campusbackend.complaint.controller.admin;

import com.sit.campusbackend.complaint.dto.ComplaintResponse;
import com.sit.campusbackend.complaint.dto.DashboardStatsResponse;
import com.sit.campusbackend.complaint.dto.StatusUpdateRequest;
import com.sit.campusbackend.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class AdminController {

    private final ComplaintService complaintService;

    public AdminController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping("/complaints")
    public ResponseEntity<Page<ComplaintResponse>> getAllComplaints(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(complaintService.getAllComplaints(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(complaintService.getDashboardStats());
    }

    @PutMapping("/status")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @Valid @RequestBody StatusUpdateRequest request) {
        com.sit.campusbackend.complaint.entity.ComplaintStatus statusEnum = 
            com.sit.campusbackend.complaint.entity.ComplaintStatus.valueOf(request.status().toUpperCase());
        return ResponseEntity.ok(complaintService.updateStatus(request.complaintId(), statusEnum));
    }

    @GetMapping("/all-complaints")
    public ResponseEntity<List<ComplaintResponse>> getAllComplaintsList() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @GetMapping("/users")
    public ResponseEntity<List<com.sit.campusbackend.auth.entity.Student>> getAllStudents() {
        return ResponseEntity.ok(complaintService.getAllStudents());
    }

    @GetMapping("/depts")
    public ResponseEntity<List<com.sit.campusbackend.complaint.entity.Department>> getAllDepts() {
        return ResponseEntity.ok(complaintService.getAllDepartments());
    }
}
