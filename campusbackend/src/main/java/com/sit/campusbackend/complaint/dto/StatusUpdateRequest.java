package com.sit.campusbackend.complaint.dto;

import com.sit.campusbackend.complaint.entity.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotNull(message = "complaintId is required")
    private Long complaintId;

    @NotNull(message = "status is required")
    private ComplaintStatus status;
}
