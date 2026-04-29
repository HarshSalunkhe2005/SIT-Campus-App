package com.sit.campusbackend.complaint.dto;
import jakarta.validation.constraints.NotBlank;
public record StatusUpdateRequest(
    Long complaintId,
    @NotBlank(message="Status required") String status
) {}
