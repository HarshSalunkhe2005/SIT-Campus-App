package com.sit.campusbackend.auth.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Represents a registered student.
 * passwordHash is stored as a BCrypt hash — never plain text.
 */
@Entity
@Table(name = "students")
@Data
public class Student {

    /** Primary key — institutional email (e.g. john.doe.2023@sitpune.edu.in). */
    @Id
    @Column(nullable = false, unique = true)
    private String email;

    /** Student's PRN (Permanent Registration Number). */
    @Column
    private String prn;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    /** Year extracted from email, e.g. "2023". */
    @Column
    private String batchYear;

    /** BCrypt hash of the student's password. Set after OTP verification. */
    @Column
    private String passwordHash;

    /** Becomes true once the student's OTP is verified. */
    @Column(nullable = false)
    private Boolean isVerified = false;
}
