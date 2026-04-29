package com.sit.campusbackend.complaint.controller.department;

import com.sit.campusbackend.complaint.dto.ComplaintResponse;
import com.sit.campusbackend.complaint.dto.LoginRequest;
import com.sit.campusbackend.complaint.dto.LoginResponse;
import com.sit.campusbackend.complaint.dto.ResolveRequest;
import com.sit.campusbackend.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Department-facing endpoints.
 * Departments log in, view their queue, and mark complaints resolved.
 * Base path: /dept
 */
@RestController
@RequestMapping("/dept")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class DepartmentController {

    private final ComplaintService complaintService;

    public DepartmentController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * POST /dept/login
     *
     * Authenticates a department using email + BCrypt password.
     * Body: { "email": "it@sitpune.edu.in", "password": "dept@123" }
     * Response: { "role": "DEPARTMENT", "departmentId": 2,
     *             "departmentName": "IT Department", "token": "jwt.token.here" }
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = complaintService.departmentLogin(
                request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /dept/complaints/{departmentId}
     *
     * Returns all complaints assigned to this department.
     */
    @GetMapping("/complaints/{departmentId}")
    public ResponseEntity<List<ComplaintResponse>> getDepartmentComplaints(
            @PathVariable Long departmentId) {

        return ResponseEntity.ok(complaintService.getDepartmentComplaints(departmentId));
    }

    /**
     * PUT /dept/resolve
     *
     * Mark a complaint RESOLVED, attach a proof image URL, and notify the student.
     * Body: { "complaintId": 1, "resolvedImageUrl": "https://..." }
     */
    @PutMapping("/resolve")
    public ResponseEntity<ComplaintResponse> resolveComplaint(
            @Valid @RequestBody ResolveRequest request) {

        ComplaintResponse resolved = complaintService.resolveComplaint(
                request.getComplaintId(), request.getResolvedImageUrl());
        return ResponseEntity.ok(resolved);
    }
}
