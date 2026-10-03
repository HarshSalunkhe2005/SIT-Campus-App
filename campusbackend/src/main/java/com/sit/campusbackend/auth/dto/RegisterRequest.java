package com.sit.campusbackend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code prn} may be omitted when re-sending a code to an already started registration. */
public record RegisterRequest(
    @NotBlank(message = "Email required") String email,
    String prn
) {}
