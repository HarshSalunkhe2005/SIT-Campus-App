package com.sit.campusbackend.auth.controller;

import com.sit.campusbackend.auth.service.AuthService;
import com.sit.campusbackend.complaint.dto.LoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> request) {
        authService.registerStudent(request.get("email"), request.get("prn"));
        return ResponseEntity.ok(Map.of("message", "OTP sent to " + request.get("email")));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody Map<String, String> request) {
        authService.verifyOtp(request.get("email"), request.get("otp"));
        return ResponseEntity.ok(Map.of("message", "Email verified successfully."));
    }

    @PostMapping("/set-password")
    public ResponseEntity<Map<String, String>> setPassword(@RequestBody Map<String, String> body) {
        authService.setPassword(body.get("email"), body.get("password"));
        return ResponseEntity.ok(Map.of("message", "Account created successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.password()));
    }
}
