package com.sit.campusbackend.complaint.controller.department;

import com.sit.campusbackend.complaint.dto.ComplaintResponse;
import com.sit.campusbackend.complaint.dto.ResolveRequest;
import com.sit.campusbackend.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dept")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class DepartmentController {

    private final ComplaintService complaintService;

    public DepartmentController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping("/queue/{departmentId}")
    public ResponseEntity<List<ComplaintResponse>> getDepartmentComplaints(
            @PathVariable Long departmentId) {
        return ResponseEntity.ok(complaintService.getDepartmentComplaints(departmentId));
    }

    @PostMapping("/{complaintId}/resolve")
    public ResponseEntity<ComplaintResponse> resolveComplaint(
            @PathVariable Long complaintId,
            @Valid @RequestBody ResolveRequest request) {
        return ResponseEntity.ok(complaintService.resolveComplaint(complaintId, request.resolutionNotes()));
    }

    @PutMapping("/status")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @Valid @RequestBody com.sit.campusbackend.complaint.dto.StatusUpdateRequest request) {
        com.sit.campusbackend.complaint.entity.ComplaintStatus statusEnum = 
            com.sit.campusbackend.complaint.entity.ComplaintStatus.valueOf(request.status().toUpperCase());
        return ResponseEntity.ok(complaintService.updateStatus(request.complaintId(), statusEnum));
    }
}
