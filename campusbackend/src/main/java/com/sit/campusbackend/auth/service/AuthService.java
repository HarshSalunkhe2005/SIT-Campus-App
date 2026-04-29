package com.sit.campusbackend.auth.service;

import com.sit.campusbackend.auth.entity.Admin;
import com.sit.campusbackend.auth.entity.Student;
import com.sit.campusbackend.auth.repository.AdminRepository;
import com.sit.campusbackend.auth.repository.StudentRepository;
import com.sit.campusbackend.auth.security.JwtUtil;
import com.sit.campusbackend.complaint.entity.Department;
import com.sit.campusbackend.complaint.repository.DepartmentRepository;
import com.sit.campusbackend.complaint.exception.ResourceNotFoundException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final DepartmentRepository departmentRepository;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();

    public AuthService(StudentRepository studentRepository, AdminRepository adminRepository,
                       DepartmentRepository departmentRepository, JavaMailSender mailSender,
                       BCryptPasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.studentRepository = studentRepository;
        this.adminRepository = adminRepository;
        this.departmentRepository = departmentRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public void registerStudent(String email, String prn) {
        if (email == null || !email.endsWith("@sitpune.edu.in")) {
            throw new IllegalArgumentException("Invalid SIT email. Use your @sitpune.edu.in address.");
        }
        Optional<Student> existing = studentRepository.findById(email);
        if (existing.isPresent() && existing.get().getPasswordHash() != null) {
            throw new IllegalArgumentException("User already exists. Please login.");
        }

        String namePart = email.split("@")[0];
        String[] parts = namePart.split("\\.");
        String firstName = parts.length > 0 ? capitalize(parts[0]) : "Student";
        String lastName = parts.length > 1 ? capitalize(parts[1]) : "";
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
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your OTP Code — Campus Portal");
        message.setText("Your verification code is: " + otp + "\n\nThis OTP expires when used.");
        mailSender.send(message);
    }

    public void verifyOtp(String email, String otp) {
        String savedOtp = otpStorage.get(email);
        if (savedOtp == null) throw new IllegalArgumentException("No OTP found. Register first.");
        if (!savedOtp.equals(otp)) throw new IllegalArgumentException("Wrong OTP.");

        Student student = studentRepository.findById(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        student.setIsVerified(true);
        studentRepository.save(student);
        otpStorage.remove(email);
    }

    public void setPassword(String email, String rawPassword) {
        if (email == null || rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Email and password required");
        }
        Student student = studentRepository.findById(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        student.setPasswordHash(passwordEncoder.encode(rawPassword));
        student.setIsVerified(true);
        studentRepository.save(student);
    }

    public Map<String, String> login(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            throw new IllegalArgumentException("Email and password required");
        }

        Optional<Admin> adminOpt = adminRepository.findByEmail(email);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            if (admin.getPasswordHash() == null || !passwordEncoder.matches(rawPassword, admin.getPasswordHash())) {
                throw new IllegalArgumentException("Invalid credentials");
            }
            return Map.of("role", "ADMIN", "email", email, "token", jwtUtil.generateToken(email, "ADMIN"));
        }

        Optional<Department> deptOpt = departmentRepository.findByEmail(email);
        if (deptOpt.isPresent()) {
            Department dept = deptOpt.get();
            if (dept.getPasswordHash() == null || !passwordEncoder.matches(rawPassword, dept.getPasswordHash())) {
                throw new IllegalArgumentException("Invalid credentials");
            }
            return Map.of("role", "DEPARTMENT", "email", email, "departmentId", String.valueOf(dept.getId()),
                          "departmentName", dept.getName(), "token", jwtUtil.generateToken(email, "DEPARTMENT"));
        }

        Student student = studentRepository.findById(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        if (student.getIsVerified() == null || !student.getIsVerified()) {
            throw new IllegalArgumentException("Please verify email first.");
        }
        if (student.getPasswordHash() == null || !passwordEncoder.matches(rawPassword, student.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid password");
        }
        return Map.of("role", "STUDENT", "email", email, "token", jwtUtil.generateToken(email, "STUDENT"));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
