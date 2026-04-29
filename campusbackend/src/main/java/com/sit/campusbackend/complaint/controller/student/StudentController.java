package com.sit.campusbackend.complaint.controller.student;

import com.sit.campusbackend.complaint.dto.ComplaintRequest;
import com.sit.campusbackend.complaint.dto.ComplaintResponse;
import com.sit.campusbackend.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Student-facing endpoints.
 * Base path: /student
 */
@RestController
@RequestMapping("/student")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class StudentController {

    private final ComplaintService complaintService;

    public StudentController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * POST /student/complaint
     *
     * Submit a new complaint. All @NotBlank / @Size / @Email constraints
     * in ComplaintRequest are enforced by @Valid.
     *
     * Body: { email, title, description, imageUrl?, priority? }
     * Returns: 201 Created with full ComplaintResponse.
     */
    @PostMapping("/complaint")
    public ResponseEntity<ComplaintResponse> submitComplaint(
            @Valid @RequestBody ComplaintRequest request) {

        ComplaintResponse response = complaintService.createComplaint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /student/complaints/{email}
     *
     * Fetch all complaints submitted by the given student.
     * Returns: 200 OK with list (empty list when no complaints exist).
     */
    @GetMapping("/complaints/{email}")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(
            @PathVariable String email) {

        return ResponseEntity.ok(complaintService.getStudentComplaints(email));
    }
}
