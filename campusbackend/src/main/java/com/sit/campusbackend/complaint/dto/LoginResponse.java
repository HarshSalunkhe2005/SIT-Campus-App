package com.sit.campusbackend.complaint.dto;
public record LoginResponse(
    String role, Long id, String name, String token
) {}
