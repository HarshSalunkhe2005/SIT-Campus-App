package com.sit.campusbackend.auth.service;

import com.sit.campusbackend.auth.entity.Student;
import com.sit.campusbackend.auth.repository.AdminRepository;
import com.sit.campusbackend.auth.repository.StudentRepository;
import com.sit.campusbackend.auth.security.JwtUtil;
import com.sit.campusbackend.common.MailService;
import com.sit.campusbackend.complaint.entity.Department;
import com.sit.campusbackend.complaint.repository.DepartmentRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final String STUDENT_DOMAIN = "@sitpune.edu.in";
    private static final Pattern PRN = Pattern.compile("^\\d{11}$");
    private static final String BAD_CREDENTIALS = "Invalid credentials";

    private final StudentRepository students;
    private final AdminRepository admins;
    private final DepartmentRepository departments;
    private final MailService mail;
    private final OtpService otps;
    private final LoginRateLimiter rateLimiter;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** Compared against when the account does not exist, so response time does not reveal which emails are registered. */
    private final String dummyHash;

    public AuthService(StudentRepository students, AdminRepository admins, DepartmentRepository departments,
                       MailService mail, OtpService otps, LoginRateLimiter rateLimiter,
                       BCryptPasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.students = students;
        this.admins = admins;
        this.departments = departments;
        this.mail = mail;
        this.otps = otps;
        this.rateLimiter = rateLimiter;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    // ── registration: register (sends OTP) -> verify-otp -> set-password ────────────────────────

    public void registerStudent(String rawEmail, String rawPrn) {
        String email = normalize(rawEmail);
        if (!email.endsWith(STUDENT_DOMAIN)) {
            throw new IllegalArgumentException("Invalid SIT email. Use your @sitpune.edu.in address.");
        }
        Student existing = students.findById(email).orElse(null);
        if (existing != null && existing.getPasswordHash() != null) {
            throw new IllegalArgumentException("User already exists. Please login.");
        }

        String prn = rawPrn == null ? "" : rawPrn.trim();
        if ((existing == null || !prn.isEmpty()) && !PRN.matcher(prn).matches()) {
            throw new IllegalArgumentException("PRN must be 11 numeric digits.");
        }

        String otp = otps.issue(email);
        try {
            Student student = existing != null ? existing : newStudent(email);
            if (!prn.isEmpty()) student.setPrn(prn);
            student.setIsVerified(false);
            students.save(student);
            mail.sendOtp(email, otp);
        } catch (RuntimeException e) {
            otps.revoke(email);
            throw e;
        }
    }

    public void verifyOtp(String rawEmail, String otp) {
        String email = normalize(rawEmail);
        otps.verify(email, otp == null ? null : otp.trim());
        Student student = students.findById(email)
                .orElseThrow(() -> new IllegalArgumentException("No OTP found. Register first."));
        student.setIsVerified(true);
        students.save(student);
    }

    public void setPassword(String rawEmail, String rawPassword) {
        String email = normalize(rawEmail);
        checkPasswordStrength(rawPassword);
        if (!otps.isVerified(email)) {
            throw new IllegalArgumentException("Verify your email with the OTP first.");
        }
        Student student = students.findById(email)
                .orElseThrow(() -> new IllegalArgumentException("Verify your email with the OTP first."));
        if (student.getPasswordHash() != null) {
            throw new IllegalArgumentException("User already exists. Please login.");
        }
        student.setPasswordHash(passwordEncoder.encode(rawPassword));
        student.setIsVerified(true);
        students.save(student);
        otps.clearVerified(email);
    }

    // ── login ────────────────────────────────────────────────────────────────────────────────────

    public Map<String, String> login(String rawEmail, String rawPassword, String clientAddress) {
        String email = normalize(rawEmail);
        rateLimiter.checkAllowed(email, clientAddress);

        Map<String, String> result = authenticate(email, rawPassword);
        if (result == null) {
            rateLimiter.recordFailure(email, clientAddress);
            throw new IllegalArgumentException(BAD_CREDENTIALS);
        }
        rateLimiter.recordSuccess(email);
        return result;
    }

    /** Returns the login response, or null when the credentials are wrong. */
    private Map<String, String> authenticate(String email, String password) {
        var admin = admins.findByEmailIgnoreCase(email);
        if (admin.isPresent()) {
            if (!matches(password, admin.get().getPasswordHash())) return null;
            String stored = admin.get().getEmail();
            return Map.of("role", "ADMIN", "email", stored, "token", jwtUtil.generateToken(stored, "ADMIN"));
        }

        Optional<Department> dept = departments.findByEmailIgnoreCase(email);
        if (dept.isPresent()) {
            Department d = dept.get();
            if (!matches(password, d.getPasswordHash())) return null;
            return Map.of("role", "DEPARTMENT", "email", d.getEmail(), "departmentId", String.valueOf(d.getId()),
                    "departmentName", d.getName(), "token", jwtUtil.generateToken(d.getEmail(), "DEPARTMENT"));
        }

        Optional<Student> student = students.findById(email);
        if (!matches(password, student.map(Student::getPasswordHash).orElse(null))) return null;
        Student s = student.get();
        if (!Boolean.TRUE.equals(s.getIsVerified())) {
            throw new IllegalArgumentException("Your account is disabled. Please contact the administrator.");
        }
        return Map.of("role", "STUDENT", "email", email, "name", s.getFirstName(), "token", jwtUtil.generateToken(email, "STUDENT"));
    }

    private boolean matches(String rawPassword, String hash) {
        boolean ok = passwordEncoder.matches(rawPassword, hash != null ? hash : dummyHash);
        return ok && hash != null;
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────────────

    private Student newStudent(String email) {
        String[] parts = email.substring(0, email.indexOf('@')).split("\\.");
        Student s = new Student();
        s.setEmail(email);
        s.setFirstName(parts.length > 0 && !parts[0].isEmpty() ? capitalize(parts[0]) : "Student");
        s.setLastName(parts.length > 1 ? capitalize(parts[1]) : "");
        s.setBatchYear(parts.length > 2 ? parts[2] : "Unknown");
        return s;
    }

    /** Same rule as the sign-up page: at least 8 characters and at least two of upper/digit/symbol/length. */
    private void checkPasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        int score = 1;
        if (password.chars().anyMatch(Character::isUpperCase)) score++;
        if (password.chars().anyMatch(Character::isDigit)) score++;
        if (password.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) score++;
        if (score < 2) {
            throw new IllegalArgumentException("Password is too weak: add an uppercase letter, a number or a symbol.");
        }
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }
}
