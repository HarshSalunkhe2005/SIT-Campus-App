package com.sit.campusbackend.complaint.controller.student;

import com.sit.campusbackend.complaint.dto.ComplaintRequest;
import com.sit.campusbackend.complaint.dto.ComplaintResponse;
import com.sit.campusbackend.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/student")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class StudentController {

    private final ComplaintService complaintService;
    private final com.sit.campusbackend.auth.security.JwtUtil jwtUtil;

    public StudentController(ComplaintService complaintService, com.sit.campusbackend.auth.security.JwtUtil jwtUtil) {
        this.complaintService = complaintService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/report")
    public ResponseEntity<ComplaintResponse> submitComplaint(
            @Valid @RequestPart("complaint") ComplaintRequest request,
            @RequestPart("image") MultipartFile image,
            @RequestHeader("Authorization") String token) {

        String email = extractEmailFromToken(token);
        return ResponseEntity.ok(complaintService.createComplaint(request, image, email));
    }

    @GetMapping("/my-reports")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(
            @RequestHeader("Authorization") String token) {

        String email = extractEmailFromToken(token);
        return ResponseEntity.ok(complaintService.getStudentComplaints(email));
    }



    private String extractEmailFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid or missing Authorization header");
        }
        return jwtUtil.validateAndGetEmail(authHeader.substring(7));
    }
}
