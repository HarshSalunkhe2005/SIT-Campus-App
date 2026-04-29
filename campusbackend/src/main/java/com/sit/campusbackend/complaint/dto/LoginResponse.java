package com.sit.campusbackend.complaint.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    /** The role of the authenticated entity, e.g. "DEPARTMENT". */
    private String role;

    /** Department's unique ID — used by frontend to scope API calls. */
    private Long departmentId;

    /** Department's display name, e.g. "IT Department". */
    private String departmentName;

    /**
     * Placeholder for future JWT token.
     * TODO: populate once JwtUtil is complete.
     */
    private String token;
}
