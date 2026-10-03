package com.sit.campusbackend.auth.security;

import com.sit.campusbackend.auth.repository.AdminRepository;
import com.sit.campusbackend.auth.repository.StudentRepository;
import com.sit.campusbackend.complaint.repository.DepartmentRepository;
import org.springframework.stereotype.Component;

/**
 * Checks that the account behind a valid token still exists and is enabled, so disabling or deleting
 * a user takes effect immediately instead of when their token expires.
 */
@Component
public class AccountGuard {

    private final StudentRepository students;
    private final AdminRepository admins;
    private final DepartmentRepository departments;

    public AccountGuard(StudentRepository students, AdminRepository admins, DepartmentRepository departments) {
        this.students = students;
        this.admins = admins;
        this.departments = departments;
    }

    public boolean isActive(String role, String email) {
        return switch (role) {
            case "STUDENT" -> students.existsByEmailAndIsVerifiedTrue(email);
            case "ADMIN" -> admins.existsByEmailIgnoreCase(email);
            case "DEPARTMENT" -> departments.existsByEmailIgnoreCase(email);
            default -> false;
        };
    }
}
