package com.sit.campusbackend.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResolveRequest {

    @NotNull(message = "complaintId is required")
    private Long complaintId;

    @NotBlank(message = "resolvedImageUrl is required")
    private String resolvedImageUrl;
}
