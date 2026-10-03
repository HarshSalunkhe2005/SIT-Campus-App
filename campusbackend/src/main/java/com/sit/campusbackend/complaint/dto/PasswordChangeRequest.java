package com.sit.campusbackend.complaint.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
    @NotBlank(message = "Current password required") String currentPassword,
    @NotBlank(message = "New password required") String newPassword
) {}
