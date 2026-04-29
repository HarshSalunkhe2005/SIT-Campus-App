package com.sit.campusbackend.complaint.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "departments")
@Data
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** e.g. "Electrical", "IT", "Cleaning", "Hostel", "Plumbing", "General" */
    @Column(unique = true, nullable = false)
    private String type;

    @Column(nullable = false)
    private String email;

    /** BCrypt hash of the department's login password (set via seed SQL). */
    private String passwordHash;
}
