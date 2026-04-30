package com.sit.campusbackend.complaint.dto;
import com.sit.campusbackend.complaint.entity.ComplaintPriority;
import com.sit.campusbackend.complaint.entity.ComplaintStatus;
import java.time.LocalDateTime;

public record ComplaintResponse(
    Long id, String location, String description, String category,
    String imageUrl, String resolvedImageUrl, ComplaintStatus status,
    ComplaintPriority priority, LocalDateTime createdAt, LocalDateTime updatedAt,
    String studentEmail, String studentName, String departmentName,
    int upvoteCount
) {}
