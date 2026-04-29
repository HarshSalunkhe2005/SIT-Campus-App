package com.sit.campusbackend.complaint.dto;

import com.sit.campusbackend.complaint.entity.ComplaintPriority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ComplaintRequest {

    @NotBlank(message = "Student email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    /** Optional — Supabase image URL. Null when no photo is attached. */
    private String imageUrl;

    /**
     * Optional priority. Accepted values: LOW, MEDIUM, HIGH.
     * Defaults to MEDIUM in ComplaintService when not provided.
     */
    private ComplaintPriority priority;
}
