package com.sit.campusbackend.auth.controller;

import com.sit.campusbackend.auth.entity.Student;
import com.sit.campusbackend.auth.repository.StudentRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final StudentRepository studentRepository;
    private final JavaMailSender mailSender;
    private String savedOtp = "";

    public AuthController(StudentRepository studentRepository,
                          JavaMailSender mailSender) {
        this.studentRepository = studentRepository;
        this.mailSender = mailSender;
    }

    // get all students
    @GetMapping("/all")
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    // add student
    @PostMapping("/add")
    public Student addStudent(@RequestBody Student student) {
        return studentRepository.save(student);
    }

    // login
    @PostMapping("/login")
    public String login(@RequestBody Student loginData){

        Student student = studentRepository
                .findById(loginData.getEmail())
                .orElse(null);

        if(student == null){
            return "Please register first";
        }

        if(!student.getPasswordHash().equals(loginData.getPasswordHash())){
            return "Wrong password";
        }

        if(student.getIsVerified() == null || !student.getIsVerified()){
            return "Please verify your email first";
        }

        return "Login success";
    }

    // generate OTP
    @PostMapping("/generate-otp")
    public String generateOtp(@RequestBody Student student){

        Random random = new Random();
        savedOtp = String.valueOf(random.nextInt(900000) + 100000);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(student.getEmail());
        message.setSubject("Your Verification OTP");
        message.setText("Your OTP is: " + savedOtp);
        message.setFrom("rafinuzzaman110@gmail.com");

        mailSender.send(message);

        return "OTP sent";
    }

    // verify OTP
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestBody Student student){

        if(savedOtp.equals(student.getOtp())){

            student.setIsVerified(true);
            studentRepository.save(student);

            return "Student registered successfully";
        } else {
            return "Wrong OTP";
        }
    }
}