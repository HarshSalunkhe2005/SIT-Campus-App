package com.sit.campusbackend.complaint.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Returned by GET /admin/stats.
 * Gives an at-a-glance view of the complaint pipeline health.
 */
@Data
@AllArgsConstructor
public class DashboardStatsResponse {
    private long total;
    private long pending;
    private long assigned;
    private long inProgress;
    private long resolved;
    private long closed;
}
