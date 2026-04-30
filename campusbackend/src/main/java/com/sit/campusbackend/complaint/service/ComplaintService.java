package com.sit.campusbackend.complaint.service;

import com.sit.campusbackend.auth.entity.Student;
import com.sit.campusbackend.auth.repository.StudentRepository;
import com.sit.campusbackend.complaint.dto.*;
import com.sit.campusbackend.complaint.entity.*;
import com.sit.campusbackend.complaint.exception.ResourceNotFoundException;
import com.sit.campusbackend.complaint.repository.ComplaintRepository;
import com.sit.campusbackend.complaint.repository.DepartmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.sit.campusbackend.auth.security.JwtUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Core service for the Campus Complaint Management System.
 *
 * Responsibilities:
 *  1. Create complaint — auto-detect category, assign department, set priority
 *  2. Retrieve complaints — by student, by department, or all (paginated)
 *  3. Update complaint status (admin)
 *  4. Resolve complaint (department) + send resolution email
 *  5. Department login — BCrypt password verification
 *  6. Dashboard stats (admin)
 */
@Service
public class ComplaintService {

    private final ComplaintRepository   complaintRepository;
    private final DepartmentRepository  departmentRepository;
    private final StudentRepository     studentRepository;
    private final JavaMailSender        mailSender;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil               jwtUtil;

    public ComplaintService(ComplaintRepository complaintRepository,
                            DepartmentRepository departmentRepository,
                            StudentRepository studentRepository,
                            JavaMailSender mailSender,
                            BCryptPasswordEncoder passwordEncoder,
                            JwtUtil jwtUtil) {
        this.complaintRepository  = complaintRepository;
        this.departmentRepository = departmentRepository;
        this.studentRepository    = studentRepository;
        this.mailSender           = mailSender;
        this.passwordEncoder      = passwordEncoder;
        this.jwtUtil              = jwtUtil;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. CREATE — auto-detect category, assign department, set priority
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Create and persist a new complaint.
     *
     * Flow:
     *  a. Validate inputs (also enforced via @Valid in controller).
     *  b. Detect category from description keywords.
     *  c. Find matching department; fall back to "General" if not found.
     *  d. Default priority to MEDIUM when not provided.
     *  e. Set initial status to ASSIGNED.
     */
    public ComplaintResponse createComplaint(ComplaintRequest req, org.springframework.web.multipart.MultipartFile image, String email) {
        if (email == null || req.location() == null || req.description() == null) {
            throw new IllegalArgumentException("email, location, and description are required");
        }

        Student student = studentRepository.findById(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + email));

        String category = detectCategory(req.description());
        Department dept = departmentRepository.findByType(category)
                .orElseGet(() -> departmentRepository.findByType("General")
                        .orElseThrow(() -> new ResourceNotFoundException("No department configured for category: " + category)));

        ComplaintPriority priority = req.priority() != null ? req.priority() : ComplaintPriority.MEDIUM;

        Complaint complaint = new Complaint();
        complaint.setStudent(student);
        complaint.setTitle(req.location().trim());
        complaint.setDescription(req.description().trim());
        complaint.setCategory(category);
        complaint.setImageUrl(req.imageUrl());
        complaint.setDepartment(dept);
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        complaint.setPriority(priority);

        return toResponse(complaintRepository.save(complaint));
    }

    public List<ComplaintResponse> getStudentComplaints(String email) {
        return complaintRepository.findByStudentEmail(email)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Page<ComplaintResponse> getAllComplaints(Pageable pageable) {
        return complaintRepository.findAll(pageable).map(this::toResponse);
    }

    public List<ComplaintResponse> getAllComplaints() {
        return complaintRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ComplaintResponse updateStatus(Long complaintId, ComplaintStatus status) {
        Complaint complaint = findComplaint(complaintId);
        complaint.setStatus(status);
        return toResponse(complaintRepository.save(complaint));
    }

    public DashboardStatsResponse getDashboardStats() {
        return new DashboardStatsResponse(
            complaintRepository.count(),
            complaintRepository.countByStatus(ComplaintStatus.PENDING),
            complaintRepository.countByStatus(ComplaintStatus.ASSIGNED),
            complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS),
            complaintRepository.countByStatus(ComplaintStatus.RESOLVED),
            complaintRepository.countByStatus(ComplaintStatus.CLOSED)
        );
    }

    public List<ComplaintResponse> getDepartmentComplaints(Long departmentId) {
        return complaintRepository.findByDepartmentId(departmentId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public ComplaintResponse resolveComplaint(Long complaintId, String resolvedImageUrl) {
        Complaint complaint = findComplaint(complaintId);
        complaint.setResolvedImageUrl(resolvedImageUrl);
        complaint.setStatus(ComplaintStatus.RESOLVED);
        ComplaintResponse saved = toResponse(complaintRepository.save(complaint));

        sendResolutionEmail(complaint.getStudent().getEmail(), complaint.getTitle());
        return saved;
    }

    public ComplaintResponse upvoteComplaint(Long complaintId) {
        Complaint complaint = findComplaint(complaintId);
        complaint.setUpvoteCount(complaint.getUpvoteCount() + 1);
        return toResponse(complaintRepository.save(complaint));
    }

    String detectCategory(String description) {
        if (description == null || description.isBlank()) return "General";
        String text = description.toLowerCase();

        if (containsAny(text, "fan", "light", "electricity", "switch", "socket", "power", "wiring", "bulb", "voltage")) return "Electrical";
        if (containsAny(text, "wifi", "internet", "network", "router", "laptop", "computer", "printer", "server", "cable", "portal", "system", "connection")) return "IT";
        if (containsAny(text, "clean", "garbage", "waste", "trash", "dirt", "sweep", "toilet", "bathroom", "dustbin", "smell", "hygiene")) return "Cleaning";
        if (containsAny(text, "pipe", "water", "tap", "leak", "drain", "plumber", "flush", "seepage")) return "Plumbing";
        if (containsAny(text, "hostel", "room", "bed", "mattress", "mess", "canteen", "food", "warden", "dorm")) return "Hostel";

        return "General";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private Complaint findComplaint(Long id) {
        return complaintRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));
    }

    private void sendResolutionEmail(String to, String complaintTitle) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Complaint Resolved — " + complaintTitle);
        msg.setText("Dear Student,\n\nYour complaint \"" + complaintTitle + "\" has been successfully resolved.\n\nIf the issue persists, please raise a new complaint with updated details.\n\nRegards,\nCampus Management Team");
        mailSender.send(msg);
    }

    public void deleteComplaint(Long id) {
        if (!complaintRepository.existsById(id)) {
            throw new ResourceNotFoundException("Complaint not found with id: " + id);
        }
        complaintRepository.deleteById(id);
    }

    private ComplaintResponse toResponse(Complaint c) {
        String studentEmail = c.getStudent() != null ? c.getStudent().getEmail() : null;
        String studentName = c.getStudent() != null ? c.getStudent().getFirstName() + " " + c.getStudent().getLastName() : null;
        String deptName = c.getDepartment() != null ? c.getDepartment().getName() : null;
        
        return new ComplaintResponse(
            c.getId(), c.getTitle(), c.getDescription(), c.getCategory(),
            c.getImageUrl(), c.getResolvedImageUrl(), c.getStatus(),
            c.getPriority(), c.getCreatedAt(), c.getUpdatedAt(),
            studentEmail, studentName, deptName, c.getUpvoteCount()
        );
    }
}
