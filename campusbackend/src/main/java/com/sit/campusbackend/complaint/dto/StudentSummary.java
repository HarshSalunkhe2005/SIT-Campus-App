package com.sit.campusbackend.complaint.dto;

import com.sit.campusbackend.auth.entity.Student;

/** What the admin user list shows about a student (never the password hash). */
public record StudentSummary(String email, String prn, String firstName, String lastName, String batchYear, boolean isVerified) {

    public static StudentSummary of(Student s) {
        return new StudentSummary(s.getEmail(), s.getPrn(), s.getFirstName(), s.getLastName(), s.getBatchYear(),
                Boolean.TRUE.equals(s.getIsVerified()));
    }
}
