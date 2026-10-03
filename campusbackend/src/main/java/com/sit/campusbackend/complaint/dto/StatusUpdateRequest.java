package com.sit.campusbackend.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
    @NotNull(message = "Complaint id required") Long complaintId,
    @NotBlank(message = "Status required") String status
) {}
