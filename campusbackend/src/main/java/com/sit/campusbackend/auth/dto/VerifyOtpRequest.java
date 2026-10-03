package com.sit.campusbackend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
    @NotBlank(message = "Email required") String email,
    @NotBlank(message = "OTP required") String otp
) {}
