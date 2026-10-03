package com.sit.campusbackend.auth.controller;

import com.sit.campusbackend.auth.dto.LoginRequest;
import com.sit.campusbackend.auth.dto.RegisterRequest;
import com.sit.campusbackend.auth.dto.SetPasswordRequest;
import com.sit.campusbackend.auth.dto.VerifyOtpRequest;
import com.sit.campusbackend.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.registerStudent(request.email(), request.prn());
        return ResponseEntity.ok(Map.of("message", "OTP sent to " + request.email().trim().toLowerCase()));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request.email(), request.otp());
        return ResponseEntity.ok(Map.of("message", "Email verified successfully."));
    }

    @PostMapping("/set-password")
    public ResponseEntity<Map<String, String>> setPassword(@Valid @RequestBody SetPasswordRequest request) {
        authService.setPassword(request.email(), request.password());
        return ResponseEntity.ok(Map.of("message", "Account created successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(authService.login(request.email(), request.password(), http.getRemoteAddr()));
    }
}
