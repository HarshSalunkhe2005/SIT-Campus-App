package com.sit.campusbackend.auth.controller;

import com.sit.campusbackend.auth.entity.Student;
import com.sit.campusbackend.auth.repository.StudentRepository;

import org.springframework.web.bind.annotation.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;
import java.util.HashMap;
import java.util.Random;
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final StudentRepository studentRepository;
    private final JavaMailSender mailSender;

    private final Map<String, String> otpStorage = new HashMap<>();

    public AuthController(StudentRepository studentRepository,
                          JavaMailSender mailSender) {
        this.studentRepository = studentRepository;
        this.mailSender = mailSender;
    }

    // REGISTER
    @PostMapping("/register")
    public String register(@RequestBody Student student) {

        student = extractDetailsFromEmail(student);
        student.setIsVerified(false);

        studentRepository.save(student);

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        otpStorage.put(student.getEmail(), otp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(student.getEmail());
        message.setSubject("Your Verification OTP");
        message.setText("Your OTP is: " + otp);
        message.setFrom("yourgmail@gmail.com");

        mailSender.send(message);

        return "OTP sent";
    }

    // VERIFY OTP
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestBody Map<String, String> request) {

        String email = request.get("email");
        String otp = request.get("otp");

        String savedOtp = otpStorage.get(email);

        if (savedOtp == null) {
            return "No OTP found";
        }

        if (savedOtp.equals(otp)) {

            Student student = studentRepository.findById(email).orElse(null);

            if (student == null) {
                return "User not found";
            }

            student.setIsVerified(true);
            studentRepository.save(student);

            otpStorage.remove(email);

            return "Verification successful";
        }

        return "Wrong OTP";
    }

    // LOGIN
    @PostMapping("/login")
    public String login(@RequestBody Student loginData) {

        Student student = studentRepository
                .findById(loginData.getEmail())
                .orElse(null);

        if (student == null) {
            return "Please register first";
        }

        if (!student.getPasswordHash()
                .equals(loginData.getPasswordHash())) {
            return "Wrong password";
        }

        if (student.getIsVerified() == null || !student.getIsVerified()) {
            return "Please verify your email first";
        }

        return "Login success";
    }

    private Student extractDetailsFromEmail(Student student) {

        String email = student.getEmail();
        String localPart = email.split("@")[0];
        String[] parts = localPart.split("\\.");

        if (parts.length < 3) {
            throw new RuntimeException("Invalid SIT email format");
        }

        String firstName = parts[0];
        String lastName = parts[1];

        String batchPart = parts[2];
        String batchYear = batchPart.replaceAll("[^0-9]", "");

        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setBatchYear(batchYear);

        return student;
    }
}