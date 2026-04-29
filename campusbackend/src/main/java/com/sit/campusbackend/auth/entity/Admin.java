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

    @Column(nullable = false)
    private String name;

    /** BCrypt hash of the admin's password. */
    @Column(nullable = false)
    private String passwordHash;
}
