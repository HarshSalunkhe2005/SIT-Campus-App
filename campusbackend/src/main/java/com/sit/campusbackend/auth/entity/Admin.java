package com.sit.campusbackend.auth.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Represents an admin user.
 * passwordHash is stored as a BCrypt hash — never plain text.
 */
@Entity
@Table(name = "admins")
@Data
public class Admin {

    @Id
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    /** BCrypt hash of the admin's password. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
}
