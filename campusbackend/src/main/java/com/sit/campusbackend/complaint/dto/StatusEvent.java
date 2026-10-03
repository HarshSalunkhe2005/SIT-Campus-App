package com.sit.campusbackend.complaint.dto;

import com.sit.campusbackend.complaint.entity.ComplaintStatus;

import java.time.LocalDateTime;

/** One entry of a complaint's progress history. */
public record StatusEvent(ComplaintStatus status, LocalDateTime at) {}
