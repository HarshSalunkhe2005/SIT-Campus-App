package com.sit.campusbackend.complaint.service;

import com.sit.campusbackend.auth.entity.Student;
import com.sit.campusbackend.auth.repository.AdminRepository;
import com.sit.campusbackend.auth.repository.StudentRepository;
import com.sit.campusbackend.complaint.dto.DepartmentRequest;
import com.sit.campusbackend.complaint.dto.DepartmentResponse;
import com.sit.campusbackend.complaint.dto.StudentSummary;
import com.sit.campusbackend.complaint.entity.Department;
import com.sit.campusbackend.complaint.exception.ResourceNotFoundException;
import com.sit.campusbackend.complaint.repository.ComplaintRepository;
import com.sit.campusbackend.complaint.repository.DepartmentRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

/** User and department management for the admin hub. */
@Service
@Transactional
public class AdminService {

    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final StudentRepository students;
    private final AdminRepository admins;
    private final DepartmentRepository departments;
    private final ComplaintRepository complaints;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public AdminService(StudentRepository students, AdminRepository admins, DepartmentRepository departments,
                        ComplaintRepository complaints, BCryptPasswordEncoder passwordEncoder) {
        this.students = students;
        this.admins = admins;
        this.departments = departments;
        this.complaints = complaints;
        this.passwordEncoder = passwordEncoder;
    }

    // ── own account ──────────────────────────────────────────────────────────────────────────────

    public void changeAdminPassword(String adminEmail, String currentPassword, String newPassword) {
        var admin = admins.findByEmailIgnoreCase(adminEmail).orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (!passwordEncoder.matches(currentPassword, admin.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        admin.setPasswordHash(passwordEncoder.encode(requireStrong(newPassword)));
        admins.save(admin);
    }

    // ── students ─────────────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StudentSummary> getStudents() {
        return students.findAll().stream().map(StudentSummary::of).toList();
    }

    /** Enables or disables a student; a disabled student can no longer log in or use an existing token. */
    public void toggleStudent(String email) {
        Student student = students.findById(email).orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        student.setIsVerified(!Boolean.TRUE.equals(student.getIsVerified()));
        students.save(student);
    }

    public void deleteStudent(String email) {
        if (!students.existsById(email)) {
            throw new ResourceNotFoundException("Student not found: " + email);
        }
        complaints.deleteByStudentEmail(email); // foreign key: their complaints go first
        students.deleteById(email);
    }

    // ── departments ──────────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartments() {
        return departments.findAll().stream().map(d -> toResponse(d, null)).toList();
    }

    public DepartmentResponse createDepartment(DepartmentRequest req) {
        boolean generated = req.password() == null || req.password().isBlank();
        String password = generated ? randomPassword() : requireStrong(req.password());

        Department dept = new Department();
        dept.setName(req.name().trim());
        dept.setType(req.type().trim());
        dept.setEmail(req.email().trim().toLowerCase(Locale.ROOT));
        dept.setPasswordHash(passwordEncoder.encode(password));
        return toResponse(departments.saveAndFlush(dept), generated ? password : null);
    }

    public DepartmentResponse updateDepartment(Long id, DepartmentRequest req) {
        Department dept = departments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dept not found"));
        dept.setName(req.name().trim());
        dept.setType(req.type().trim());
        dept.setEmail(req.email().trim().toLowerCase(Locale.ROOT));
        if (req.password() != null && !req.password().isBlank()) {
            dept.setPasswordHash(passwordEncoder.encode(requireStrong(req.password())));
        }
        return toResponse(departments.saveAndFlush(dept), null);
    }

    public void deleteDepartment(Long id) {
        if (!departments.existsById(id)) {
            throw new ResourceNotFoundException("Department not found: " + id);
        }
        complaints.deleteByDepartmentId(id); // foreign key: complaints assigned to it go first
        departments.deleteById(id);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────────────

    private static DepartmentResponse toResponse(Department d, String initialPassword) {
        return new DepartmentResponse(d.getId(), d.getName(), d.getType(), d.getEmail(), initialPassword);
    }

    private static String requireStrong(String password) {
        if (password.length() < 8) throw new IllegalArgumentException("Password must be at least 8 characters.");
        return password;
    }

    private String randomPassword() {
        StringBuilder sb = new StringBuilder(14);
        for (int i = 0; i < 14; i++) sb.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
        return sb.toString();
    }
}
