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

/**
 * Admin-facing endpoints.
 * Admin monitors the system and can push status changes.
 * Base path: /admin
 */
@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class AdminController {

    private final ComplaintService complaintService;

    public AdminController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * GET /admin/complaints?page=0&size=20
     *
     * Returns a paginated list of all complaints, newest first.
     * Defaults: page=0, size=20.
     *
     * Response shape (Spring Page):
     * {
     *   "content": [...],
     *   "totalPages": 5,
     *   "totalElements": 98,
     *   "number": 0,
     *   "size": 20
     * }
     */
    @GetMapping("/complaints")
    public ResponseEntity<Page<ComplaintResponse>> getAllComplaints(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ComplaintResponse> result = complaintService.getAllComplaints(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return ResponseEntity.ok(result);
    }

    /**
     * GET /admin/stats
     *
     * Returns complaint counts grouped by status.
     * { "total": 42, "pending": 3, "assigned": 10,
     *   "inProgress": 12, "resolved": 14, "closed": 3 }
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(complaintService.getDashboardStats());
    }

    /**
     * PUT /admin/status
     *
     * Move a complaint through the pipeline (e.g. IN_PROGRESS → CLOSED).
     * Body: { "complaintId": 1, "status": "CLOSED" }
     */
    @PutMapping("/status")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @Valid @RequestBody StatusUpdateRequest request) {

        ComplaintResponse updated = complaintService.updateStatus(
                request.getComplaintId(), request.getStatus());
        return ResponseEntity.ok(updated);
    }
}
