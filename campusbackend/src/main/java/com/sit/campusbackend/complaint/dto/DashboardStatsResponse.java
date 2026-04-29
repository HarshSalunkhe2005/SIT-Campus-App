package com.sit.campusbackend.complaint.dto;
public record DashboardStatsResponse(
    long total, long pending, long assigned,
    long inProgress, long resolved, long closed
) {}
