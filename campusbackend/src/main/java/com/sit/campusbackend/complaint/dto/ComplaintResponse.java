package com.sit.campusbackend.complaint.dto;

import com.sit.campusbackend.complaint.entity.ComplaintPriority;
import com.sit.campusbackend.complaint.entity.ComplaintStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ComplaintResponse {
    private Long             id;
    private String           title;
    private String           description;
    private String           category;
    private String           imageUrl;
    private String           resolvedImageUrl;
    private ComplaintStatus  status;
    private ComplaintPriority priority;
    private String           studentEmail;
    private String           studentName;
    private String           departmentName;
    private LocalDateTime    createdAt;
    private LocalDateTime    updatedAt;
}
