package com.sit.campusbackend.complaint.controller.department;

import com.sit.campusbackend.complaint.dto.ComplaintResponse;
import com.sit.campusbackend.complaint.dto.StatusUpdateRequest;
import com.sit.campusbackend.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Every call is limited to the logged-in department's own complaints. */
@RestController
@RequestMapping("/dept")
public class DepartmentController {

    private final ComplaintService complaintService;

    public DepartmentController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping("/queue/{departmentId}")
    public ResponseEntity<List<ComplaintResponse>> getDepartmentComplaints(@PathVariable Long departmentId, Authentication auth) {
        return ResponseEntity.ok(complaintService.getDepartmentQueue(departmentId, auth.getName()));
    }

    @PutMapping("/status")
    public ResponseEntity<ComplaintResponse> updateStatus(@Valid @RequestBody StatusUpdateRequest request, Authentication auth) {
        return ResponseEntity.ok(complaintService.updateStatusAsDepartment(request.complaintId(), request.status(), auth.getName()));
    }

    @PostMapping("/{complaintId}/proof")
    public ResponseEntity<ComplaintResponse> attachProof(@PathVariable Long complaintId,
                                                         @RequestPart("image") MultipartFile image, Authentication auth) {
        return ResponseEntity.ok(complaintService.attachProof(complaintId, image, auth.getName()));
    }
}
