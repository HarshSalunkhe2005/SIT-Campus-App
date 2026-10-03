package com.sit.campusbackend.complaint.controller.admin;

import com.sit.campusbackend.complaint.dto.*;
import com.sit.campusbackend.complaint.service.AdminService;
import com.sit.campusbackend.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final ComplaintService complaintService;
    private final AdminService adminService;

    public AdminController(ComplaintService complaintService, AdminService adminService) {
        this.complaintService = complaintService;
        this.adminService = adminService;
    }

    @GetMapping("/all-complaints")
    public ResponseEntity<List<ComplaintResponse>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(complaintService.getDashboardStats());
    }

    @PutMapping("/status")
    public ResponseEntity<ComplaintResponse> updateStatus(@Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(complaintService.updateStatusAsAdmin(request.complaintId(), request.status()));
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request, Authentication auth) {
        adminService.changeAdminPassword(auth.getName(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<StudentSummary>> getAllStudents() {
        return ResponseEntity.ok(adminService.getStudents());
    }

    @PostMapping("/user/{email}/toggle")
    public ResponseEntity<Void> toggleUserStatus(@PathVariable String email) {
        adminService.toggleStudent(email);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/user/{email}")
    public ResponseEntity<Void> deleteUser(@PathVariable String email) {
        adminService.deleteStudent(email);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/depts")
    public ResponseEntity<List<DepartmentResponse>> getAllDepts() {
        return ResponseEntity.ok(adminService.getDepartments());
    }

    @PostMapping("/dept")
    public ResponseEntity<DepartmentResponse> createDept(@Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(adminService.createDepartment(request));
    }

    @PutMapping("/dept/{id}")
    public ResponseEntity<DepartmentResponse> updateDept(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(adminService.updateDepartment(id, request));
    }

    @DeleteMapping("/dept/{id}")
    public ResponseEntity<Void> deleteDept(@PathVariable Long id) {
        adminService.deleteDepartment(id);
        return ResponseEntity.ok().build();
    }
}
