package com.sit.campusbackend.auth.controller;

import com.sit.campusbackend.auth.entity.Admin;
import com.sit.campusbackend.auth.entity.Student;
import com.sit.campusbackend.auth.repository.AdminRepository;
import com.sit.campusbackend.auth.repository.StudentRepository;
import com.sit.campusbackend.complaint.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;


import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles student registration (OTP-based), OTP verification,
 * password setup, and login for both students and admins.
 *
 * Security upgrades applied:
 *  - set-password  → BCrypt encodes before persisting
 *  - login         → BCrypt matches() — never plain-text compare
 *
 * TODO: replace role strings with JWT tokens once JwtUtil is complete.
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class AuthController {

    private final StudentRepository     studentRepository;
    private final AdminRepository       adminRepository;
    private final JavaMailSender        mailSender;
    private final BCryptPasswordEncoder passwordEncoder;

    /** In-memory OTP store. Keyed by email. Thread-safe. */
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();

    public AuthController(StudentRepository studentRepository,
                          AdminRepository adminRepository,
                          JavaMailSender mailSender,
                          BCryptPasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.adminRepository   = adminRepository;
        this.mailSender        = mailSender;
        this.passwordEncoder   = passwordEncoder;
    }

    // ── Dev helper ────────────────────────────────────────────────────────────

    /** GET /auth/all — lists all students (dev use only; remove in production). */
    @GetMapping("/all")
    public ResponseEntity<?> getAllStudents() {
        return ResponseEntity.ok(studentRepository.findAll());
    }

    // ── Step 1: Register — validate SIT email, save student, send OTP ─────────

    /**
     * POST /auth/register
     * Body: { "email": "john.doe.2023@sitpune.edu.in", "prn": "22010756" }
     *
     * Validates email domain, extracts name/batch from email, saves a
     * preliminary Student record (isVerified=false), then emails a 6-digit OTP.
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String prn   = request.get("prn");

        if (email == null || !email.endsWith("@sitpune.edu.in")) {
            return ResponseEntity.badRequest().body("Invalid SIT email. Use your @sitpune.edu.in address.");
        }

        Optional<Student> existing = studentRepository.findById(email);
        if (existing.isPresent() && existing.get().getPasswordHash() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("User already exists. Please login.");
        }

        // Extract name parts from email prefix: john.doe.2023@sitpune.edu.in
        String namePart  = email.split("@")[0];
        String[] parts   = namePart.split("\\.");
        String firstName = parts.length > 0 ? capitalize(parts[0]) : "Student";
        String lastName  = parts.length > 1 ? capitalize(parts[1]) : "";
        String batchYear = parts.length > 2 ? parts[2] : "Unknown";

        Student student = existing.orElse(new Student());
        student.setEmail(email);
        student.setPrn(prn);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setBatchYear(batchYear);
        student.setIsVerified(false);
        studentRepository.save(student);

        String otp = String.valueOf(new Random().nextInt(900_000) + 100_000);
        otpStorage.put(email, otp);
        sendEmail(email, "Your OTP Code — Campus Portal",
                  "Your verification code is: " + otp + "\n\nThis OTP expires when used.");

        return ResponseEntity.ok("OTP sent to " + email);
    }

    // ── Step 2: Verify OTP ────────────────────────────────────────────────────

    /**
     * POST /auth/verify-otp
     * Body: { "email": "...", "otp": "123456" }
     *
     * Marks the student as verified so they can set a password.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody Map<String, String> request) {
        String email    = request.get("email");
        String otp      = request.get("otp");
        String savedOtp = otpStorage.get(email);

        if (savedOtp == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No OTP found for this email. Please register first.");
        }

        if (!savedOtp.equals(otp)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong OTP. Please try again.");
        }

        Student student = studentRepository.findById(email)
                .orElse(null);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found.");
        }

        student.setIsVerified(true);
        studentRepository.save(student);
        otpStorage.remove(email);

        return ResponseEntity.ok("Email verified successfully.");
    }

    // ── Step 3: Set Password ──────────────────────────────────────────────────

    /**
     * POST /auth/set-password
     * Body: { "email": "...", "password": "plainTextPassword" }
     *
     * SECURITY: password is BCrypt-encoded before storage.
     */
    @PostMapping("/set-password")
    public ResponseEntity<Map<String, String>> setPassword(@RequestBody Map<String, String> body) {
        String email       = body.get("email");
        String rawPassword = body.get("password");

        if (email == null || rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("email and password are required");
        }

        Student student = studentRepository.findById(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + email));

        // ✅ BCrypt hash — never store plain text
        student.setPasswordHash(passwordEncoder.encode(rawPassword));
        student.setIsVerified(true);
        studentRepository.save(student);

        return ResponseEntity.ok(Map.of("message", "Account created successfully"));
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * POST /auth/login
     * Body: { "email": "...", "password": "..." }
     *
     * Checks admins table first, then students table.
     * Returns: { "role": "ADMIN"|"STUDENT", "email": "..." }
     *
     * TODO: once JwtUtil is complete, add "token" to response.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String email       = body.get("email");
        String rawPassword = body.get("password");

        // Support legacy frontend that sends "passwordHash" key
        if (rawPassword == null) rawPassword = body.get("passwordHash");

        if (email == null || rawPassword == null) {
            throw new IllegalArgumentException("email and password are required");
        }

        // 1. Check admin table
        Optional<Admin> adminOpt = adminRepository.findByEmail(email);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            if (admin.getPasswordHash() == null ||
                    !passwordEncoder.matches(rawPassword, admin.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials"));
            }
            return ResponseEntity.ok(Map.of("role", "ADMIN", "email", email));
        }

        // 2. Check student table
        Student student = studentRepository.findById(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found. Please register first."));

        if (student.getIsVerified() == null || !student.getIsVerified()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Please verify your email first."));
        }

        if (student.getPasswordHash() == null ||
                !passwordEncoder.matches(rawPassword, student.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid password"));
        }

        return ResponseEntity.ok(Map.of("role", "STUDENT", "email", email));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
