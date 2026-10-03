package com.sit.campusbackend.complaint.controller.student;

import com.sit.campusbackend.complaint.dto.ComplaintRequest;
import com.sit.campusbackend.complaint.dto.ComplaintResponse;
import com.sit.campusbackend.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentController {

    private final ComplaintService complaintService;

    public StudentController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping("/report")
    public ResponseEntity<ComplaintResponse> submitComplaint(
            @Valid @RequestPart("complaint") ComplaintRequest request,
            @RequestPart("image") MultipartFile image,
            Authentication auth) {
        return ResponseEntity.ok(complaintService.createComplaint(request, image, auth.getName()));
    }

    @GetMapping("/my-reports")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(Authentication auth) {
        return ResponseEntity.ok(complaintService.getStudentComplaints(auth.getName()));
    }

    @GetMapping("/all-reports")
    public ResponseEntity<List<ComplaintResponse>> getAllReports() {
        return ResponseEntity.ok(complaintService.getFeed());
    }

    @PostMapping("/upvote/{complaintId}")
    public ResponseEntity<ComplaintResponse> upvoteComplaint(@PathVariable Long complaintId, Authentication auth) {
        return ResponseEntity.ok(complaintService.upvote(complaintId, auth.getName()));
    }
}
