package com.sit.campusbackend.complaint.dto;
import jakarta.validation.constraints.NotBlank;
public record ResolveRequest(
    @NotBlank(message="Notes required") String resolutionNotes
) {}
