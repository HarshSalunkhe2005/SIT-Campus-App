package com.sit.campusbackend.complaint.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code password} is optional: when blank on create a random one is generated; when blank on update it is unchanged. */
public record DepartmentRequest(
    @NotBlank(message = "Name required") @Size(max = 100) String name,
    @NotBlank(message = "Category required") @Size(max = 30) String type,
    @NotBlank(message = "Email required") @Email(message = "Must be a valid email address") String email,
    String password
) {}
