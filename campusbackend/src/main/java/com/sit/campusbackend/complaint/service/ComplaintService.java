package com.sit.campusbackend.complaint.service;

import com.sit.campusbackend.auth.entity.Student;
import com.sit.campusbackend.auth.repository.StudentRepository;
import com.sit.campusbackend.common.MailService;
import com.sit.campusbackend.complaint.dto.ComplaintRequest;
import com.sit.campusbackend.complaint.dto.ComplaintResponse;
import com.sit.campusbackend.complaint.dto.DashboardStatsResponse;
import com.sit.campusbackend.complaint.entity.Complaint;
import com.sit.campusbackend.complaint.entity.ComplaintPriority;
import com.sit.campusbackend.complaint.entity.ComplaintStatus;
import com.sit.campusbackend.complaint.entity.Department;
import com.sit.campusbackend.complaint.exception.ApiException;
import com.sit.campusbackend.complaint.exception.ResourceNotFoundException;
import com.sit.campusbackend.complaint.repository.ComplaintRepository;
import com.sit.campusbackend.complaint.repository.DepartmentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Reporting, tracking and moving complaints through their lifecycle. */
@Service
@Transactional
public class ComplaintService {

    /** Upper bound on one list response (the feed and the admin table load everything in one go). */
    private static final int LIST_LIMIT = 1000;

    /** Statuses a department may set; closing is the admin's call. */
    private static final Set<ComplaintStatus> DEPARTMENT_STATUSES =
            EnumSet.of(ComplaintStatus.PENDING, ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED);

    private final ComplaintRepository complaints;
    private final DepartmentRepository departments;
    private final StudentRepository students;
    private final ImageStorageService images;
    private final MailService mail;

    public ComplaintService(ComplaintRepository complaints, DepartmentRepository departments,
                            StudentRepository students, ImageStorageService images, MailService mail) {
        this.complaints = complaints;
        this.departments = departments;
        this.students = students;
        this.images = images;
        this.mail = mail;
    }

    // ── student ──────────────────────────────────────────────────────────────────────────────────

    public ComplaintResponse createComplaint(ComplaintRequest req, MultipartFile image, String studentEmail) {
        Student student = students.findById(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentEmail));

        String picked = CategoryDetector.fromPicked(req.category());
        String category = picked != null ? picked : CategoryDetector.detect(req.description());

        Department department = departments.findByType(category)
                .or(() -> departments.findByType(CategoryDetector.GENERAL))
                .orElseThrow(() -> new ResourceNotFoundException("No department configured for category: " + category));

        Complaint complaint = new Complaint();
        complaint.setStudent(student);
        complaint.setTitle(req.location().trim());
        complaint.setDescription(req.description().trim());
        complaint.setCategory(category);
        complaint.setImageUrl(images.store(image));
        complaint.setDepartment(department);
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        complaint.setPriority(req.priority() != null ? req.priority() : ComplaintPriority.MEDIUM);

        return toResponse(complaints.save(complaint), true);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getStudentComplaints(String studentEmail) {
        return complaints.findByStudentEmailOrderByCreatedAtDesc(studentEmail).stream()
                .map(c -> toResponse(c, true)).toList();
    }

    /** The campus-wide feed every student sees: reporter names are shown, email addresses are not. */
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getFeed() {
        return latest().stream().map(c -> toResponse(c, false)).toList();
    }

    /** A student can upvote an issue once; a repeat is rejected. */
    public ComplaintResponse upvote(Long complaintId, String studentEmail) {
        Complaint complaint = find(complaintId);
        if (!complaint.getUpvoters().add(studentEmail)) {
            throw new IllegalArgumentException("You have already upvoted this issue.");
        }
        complaint.setUpvoteCount(complaint.getUpvoteCount() + 1);
        return toResponse(complaints.save(complaint), false);
    }

    // ── department ───────────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getDepartmentQueue(Long departmentId, String departmentEmail) {
        Department own = requireDepartment(departmentEmail);
        if (!own.getId().equals(departmentId)) {
            throw ApiException.forbidden("You can only view your own department's complaints.");
        }
        return complaints.findByDepartmentIdOrderByCreatedAtDesc(departmentId).stream()
                .map(c -> toResponse(c, true)).toList();
    }

    public ComplaintResponse updateStatusAsDepartment(Long complaintId, String statusName, String departmentEmail) {
        ComplaintStatus status = parseStatus(statusName);
        if (!DEPARTMENT_STATUSES.contains(status)) {
            throw ApiException.forbidden("Departments can only set Pending, In Progress or Resolved.");
        }
        return changeStatus(ownedBy(complaintId, departmentEmail), status);
    }

    /** Stores the photo a department attached as proof when moving an issue forward. */
    public ComplaintResponse attachProof(Long complaintId, MultipartFile image, String departmentEmail) {
        Complaint complaint = ownedBy(complaintId, departmentEmail);
        complaint.setResolvedImageUrl(images.store(image));
        return toResponse(complaints.save(complaint), true);
    }

    // ── admin ────────────────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaints() {
        return latest().stream().map(c -> toResponse(c, true)).toList();
    }

    public ComplaintResponse updateStatusAsAdmin(Long complaintId, String statusName) {
        return changeStatus(find(complaintId), parseStatus(statusName));
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        Map<ComplaintStatus, Long> counts = new EnumMap<>(ComplaintStatus.class);
        for (Object[] row : complaints.countGroupedByStatus()) {
            counts.put((ComplaintStatus) row[0], (Long) row[1]);
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new DashboardStatsResponse(total,
                counts.getOrDefault(ComplaintStatus.PENDING, 0L),
                counts.getOrDefault(ComplaintStatus.ASSIGNED, 0L),
                counts.getOrDefault(ComplaintStatus.IN_PROGRESS, 0L),
                counts.getOrDefault(ComplaintStatus.RESOLVED, 0L),
                counts.getOrDefault(ComplaintStatus.CLOSED, 0L));
    }

    // ── internals ────────────────────────────────────────────────────────────────────────────────

    private ComplaintResponse changeStatus(Complaint complaint, ComplaintStatus status) {
        boolean newlyResolved = status == ComplaintStatus.RESOLVED && complaint.getStatus() != ComplaintStatus.RESOLVED;
        complaint.setStatus(status);
        ComplaintResponse response = toResponse(complaints.save(complaint), true);
        if (newlyResolved) {
            mail.sendResolution(complaint.getStudent().getEmail(), complaint.getTitle());
        }
        return response;
    }

    private List<Complaint> latest() {
        return complaints.findAllBy(PageRequest.of(0, LIST_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private Complaint find(Long id) {
        return complaints.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));
    }

    private Department requireDepartment(String email) {
        return departments.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.forbidden("Department account not found."));
    }

    /** The complaint, provided it belongs to the calling department. */
    private Complaint ownedBy(Long complaintId, String departmentEmail) {
        Department own = requireDepartment(departmentEmail);
        Complaint complaint = find(complaintId);
        if (complaint.getDepartment() == null || !complaint.getDepartment().getId().equals(own.getId())) {
            throw ApiException.forbidden("This complaint belongs to another department.");
        }
        return complaint;
    }

    private static ComplaintStatus parseStatus(String name) {
        try {
            return ComplaintStatus.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown status: " + name);
        }
    }

    private static ComplaintResponse toResponse(Complaint c, boolean includeEmail) {
        Student s = c.getStudent();
        return new ComplaintResponse(
                c.getId(), c.getTitle(), c.getDescription(), c.getCategory(),
                c.getImageUrl(), c.getResolvedImageUrl(), c.getStatus(),
                c.getPriority(), c.getCreatedAt(), c.getUpdatedAt(),
                includeEmail && s != null ? s.getEmail() : null,
                s != null ? (s.getFirstName() + " " + s.getLastName()).trim() : null,
                c.getDepartment() != null ? c.getDepartment().getName() : null,
                c.getUpvoteCount());
    }
}
