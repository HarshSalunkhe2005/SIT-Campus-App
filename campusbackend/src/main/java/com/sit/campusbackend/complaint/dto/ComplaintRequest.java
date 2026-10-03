package com.sit.campusbackend.complaint.dto;

import com.sit.campusbackend.complaint.entity.ComplaintPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComplaintRequest(
    @NotBlank(message = "Location required") @Size(max = 100, message = "Location must be at most 100 characters") String location,
    @NotBlank(message = "Description required") @Size(max = 500, message = "Description must be at most 500 characters") String description,
    @Size(max = 30, message = "Category is too long") String category,
    ComplaintPriority priority
) {}
