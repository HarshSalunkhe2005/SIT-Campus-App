package com.sit.campusbackend.complaint.entity;

import com.sit.campusbackend.auth.entity.Admin;
import com.sit.campusbackend.auth.entity.Student;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Data
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Auto-detected from description keywords (e.g. "IT", "Electrical"). */
    private String category;

    /** Optional image URL — uploaded via Supabase; only URL stored here. */
    private String imageUrl;

    /** Proof-of-resolution image URL set by department on resolve. */
    private String resolvedImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status = ComplaintStatus.PENDING;

    /** Priority submitted by the student. Defaults to MEDIUM if not provided. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintPriority priority = ComplaintPriority.MEDIUM;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private int upvoteCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_email", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_email")
    private Admin admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
