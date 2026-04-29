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
    public ComplaintResponse createComplaint(ComplaintRequest req) {

        // Guard — controller already validates but service should be self-contained.
        if (req.getEmail() == null || req.getTitle() == null || req.getDescription() == null) {
            throw new IllegalArgumentException("email, title, and description are required");
        }

        Student student = studentRepository.findById(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found: " + req.getEmail()));

        String     category = detectCategory(req.getDescription());
        Department dept     = departmentRepository.findByType(category)
                .orElseGet(() -> departmentRepository.findByType("General")
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No department configured for category: " + category)));

        ComplaintPriority priority = req.getPriority() != null
                ? req.getPriority()
                : ComplaintPriority.MEDIUM;

        Complaint complaint = new Complaint();
        complaint.setStudent(student);
        complaint.setTitle(req.getTitle().trim());
        complaint.setDescription(req.getDescription().trim());
        complaint.setCategory(category);
        complaint.setImageUrl(req.getImageUrl());
        complaint.setDepartment(dept);
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        complaint.setPriority(priority);

        return toResponse(complaintRepository.save(complaint));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. STUDENT — own complaints
    // ─────────────────────────────────────────────────────────────────────────

    public List<ComplaintResponse> getStudentComplaints(String email) {
        return complaintRepository.findByStudentEmail(email)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. ADMIN — all complaints
    // ─────────────────────────────────────────────────────────────────────────

    /** Paginated — preferred for admin list view. */
    public Page<ComplaintResponse> getAllComplaints(Pageable pageable) {
        return complaintRepository.findAll(pageable).map(this::toResponse);
    }

    /** Non-paginated — kept for backward compatibility with existing tests. */
    public List<ComplaintResponse> getAllComplaints() {
        return complaintRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. ADMIN — update status
    // ─────────────────────────────────────────────────────────────────────────

    public ComplaintResponse updateStatus(Long complaintId, ComplaintStatus status) {
        Complaint complaint = findComplaint(complaintId);
        complaint.setStatus(status);
        return toResponse(complaintRepository.save(complaint));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. ADMIN — dashboard stats
    // ─────────────────────────────────────────────────────────────────────────

    public DashboardStatsResponse getDashboardStats() {
        long total      = complaintRepository.count();
        long pending    = complaintRepository.countByStatus(ComplaintStatus.PENDING);
        long assigned   = complaintRepository.countByStatus(ComplaintStatus.ASSIGNED);
        long inProgress = complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS);
        long resolved   = complaintRepository.countByStatus(ComplaintStatus.RESOLVED);
        long closed     = complaintRepository.countByStatus(ComplaintStatus.CLOSED);

        return new DashboardStatsResponse(total, pending, assigned, inProgress, resolved, closed);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. DEPARTMENT — assigned complaints
    // ─────────────────────────────────────────────────────────────────────────

    public List<ComplaintResponse> getDepartmentComplaints(Long departmentId) {
        return complaintRepository.findByDepartmentId(departmentId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. DEPARTMENT — resolve complaint + email student
    // ─────────────────────────────────────────────────────────────────────────

    public ComplaintResponse resolveComplaint(Long complaintId, String resolvedImageUrl) {
        Complaint complaint = findComplaint(complaintId);
        complaint.setResolvedImageUrl(resolvedImageUrl);
        complaint.setStatus(ComplaintStatus.RESOLVED);
        ComplaintResponse saved = toResponse(complaintRepository.save(complaint));

        sendResolutionEmail(
            complaint.getStudent().getEmail(),
            complaint.getTitle()
        );
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. DEPARTMENT — login (BCrypt verification)
    // ─────────────────────────────────────────────────────────────────────────

    public LoginResponse departmentLogin(String email, String rawPassword) {
        Department dept = departmentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No department found with email: " + email));

        if (dept.getPasswordHash() == null || dept.getPasswordHash().isBlank()) {
            throw new IllegalStateException(
                    "Department account has no password configured. Contact admin.");
        }

        if (!passwordEncoder.matches(rawPassword, dept.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return new LoginResponse("DEPARTMENT", dept.getId(), dept.getName(), jwtUtil.generateToken(email, "DEPARTMENT"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PACKAGE-PRIVATE — keyword-based category detection (testable)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Detects the complaint category from description keywords.
     * Package-private so ComplaintServiceTest can call it directly.
     */
String detectCategory(String description) {
    if (description == null || description.isBlank()) return "General";
    String text = description.toLowerCase();

    if (containsAny(text, "fan", "light", "electricity", "switch",
            "socket", "power", "wiring", "bulb", "voltage"))       return "Electrical";

    if (containsAny(text, "wifi", "internet", "network", "router",
            "laptop", "computer", "printer", "server",
            "cable", "portal", "system", "connection"))            return "IT";

    if (containsAny(text, "clean", "garbage", "waste", "trash",
            "dirt", "sweep", "toilet", "bathroom",
            "dustbin", "smell", "hygiene"))                        return "Cleaning";

    // 🔥 MOVE THIS UP
    if (containsAny(text, "pipe", "water", "tap", "leak",
            "drain", "plumber", "flush", "seepage"))               return "Plumbing";

    if (containsAny(text, "hostel", "room", "bed", "mattress",
            "mess", "canteen", "food", "warden", "dorm"))          return "Hostel";

    return "General";
}

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private Complaint findComplaint(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Complaint not found with id: " + id));
    }

    private void sendResolutionEmail(String to, String complaintTitle) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Complaint Resolved — " + complaintTitle);
        msg.setText(
            "Dear Student,\n\n" +
            "Your complaint \"" + complaintTitle + "\" has been successfully resolved.\n\n" +
            "If the issue persists, please raise a new complaint with updated details.\n\n" +
            "Regards,\nCampus Management Team"
        );
        mailSender.send(msg);
    }

    /**
     * Maps a {@link Complaint} entity to a {@link ComplaintResponse} DTO.
     * Uses explicit field access to avoid LazyInitializationException on proxies.
     */
    private ComplaintResponse toResponse(Complaint c) {
        ComplaintResponse r = new ComplaintResponse();
        r.setId(c.getId());
        r.setTitle(c.getTitle());
        r.setDescription(c.getDescription());
        r.setCategory(c.getCategory());
        r.setImageUrl(c.getImageUrl());
        r.setResolvedImageUrl(c.getResolvedImageUrl());
        r.setStatus(c.getStatus());
        r.setPriority(c.getPriority());
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());

        if (c.getStudent() != null) {
            r.setStudentEmail(c.getStudent().getEmail());
            r.setStudentName(c.getStudent().getFirstName() + " " + c.getStudent().getLastName());
        }
        if (c.getDepartment() != null) {
            r.setDepartmentName(c.getDepartment().getName());
        }
        return r;
    }
}
