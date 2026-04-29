package com.sit.campusbackend.complaint.dto;
import com.sit.campusbackend.complaint.entity.ComplaintPriority;
import jakarta.validation.constraints.NotBlank;

public record ComplaintRequest(
    @NotBlank(message="Location required") String location,
    @NotBlank(message="Description required") String description,
    String imageUrl,
    ComplaintPriority priority
) {}
