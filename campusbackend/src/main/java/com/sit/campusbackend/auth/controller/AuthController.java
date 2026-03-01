package com.sit.campusbackend.auth.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.http.HttpStatus; // Added
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sit.campusbackend.auth.entity.Student;
import com.sit.campusbackend.auth.repository.StudentRepository;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class AuthController {

    private final StudentRepository studentRepository;
    private final JavaMailSender mailSender;

    private final Map<String, String> otpStorage = new HashMap<>();

    public AuthController(StudentRepository studentRepository,
                          JavaMailSender mailSender) {
        this.studentRepository = studentRepository;
        this.mailSender = mailSender;
    }

    @GetMapping("/all")
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @PostMapping("/add")
    public Student addStudent(@RequestBody Student student) {
        return studentRepository.save(student);
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String prn = request.get("prn");

        if (email == null || !email.endsWith("@sitpune.edu.in")) {
            return ResponseEntity.badRequest().body("Invalid SIT Email");
        }

        // Check if student is already fully registered
        Optional<Student> existingStudent = studentRepository.findById(email);
        if (existingStudent.isPresent() && existingStudent.get().getPasswordHash() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists. Please login.");
        }

        String namePart = email.split("@")[0];
        String[] nameParts = namePart.split("\\.");

        String firstName = nameParts.length > 0 ? nameParts[0] : "Student";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";
        String batchYear = nameParts.length > 2 ? nameParts[2] : "Unknown";

        Student student = existingStudent.orElse(new Student());
        student.setEmail(email);
        student.setPrn(prn);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setBatchYear(batchYear);
        student.setIsVerified(false);

        studentRepository.save(student);

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        otpStorage.put(email, otp);
        
        sendEmail(email, "Your OTP Code", "Your verification code is: " + otp);

        return ResponseEntity.ok("OTP sent to your email");
    }

    @PostMapping("/set-password")
    public ResponseEntity<String> setPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        Optional<Student> studentOpt = studentRepository.findById(email);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            student.setPasswordHash(password); 
            student.setIsVerified(true);
            studentRepository.save(student);
            return ResponseEntity.ok("Account created successfully");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String savedOtp = otpStorage.get(email);

        if (savedOtp == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No OTP found");

        if (savedOtp.equals(otp)) {
            Student student = studentRepository.findById(email).orElse(null);
            if (student == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");

            student.setIsVerified(true);
            studentRepository.save(student);
            otpStorage.remove(email);
            return ResponseEntity.ok("Verification successful");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong OTP");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Student loginData) {
        Student student = studentRepository.findById(loginData.getEmail()).orElse(null);
        if (student == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Please register first");

        if (student.getPasswordHash() == null || !student.getPasswordHash().equals(loginData.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong password");
        }

        if (student.getIsVerified() == null || !student.getIsVerified()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Please verify your email first");
        }
        return ResponseEntity.ok("Login success");
    }

    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}