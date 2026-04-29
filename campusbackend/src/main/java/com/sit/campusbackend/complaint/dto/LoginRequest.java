package com.sit.campusbackend.complaint.dto;
import jakarta.validation.constraints.NotBlank;
public record LoginRequest(
    @NotBlank(message="Email required") String email,
    @NotBlank(message="Password required") String password
) {}
