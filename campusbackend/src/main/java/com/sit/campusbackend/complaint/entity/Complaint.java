package com.sit.campusbackend.complaint.entity;

import com.sit.campusbackend.auth.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "complaints")
@Getter
@Setter
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The place the issue is at (what the student typed as "location"). */
    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Canonical category, e.g. "Electrical", "IT", "Cleaning". */
    private String category;

    /** Path of the photo the student attached, served from /uploads. */
    private String imageUrl;

    /** Path of the latest proof photo the department attached when moving the issue forward. */
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

    /** Students who upvoted, so each student can upvote an issue only once. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "complaint_upvoters", joinColumns = @JoinColumn(name = "complaint_id"))
    @Column(name = "student_email", nullable = false)
    private Set<String> upvoters = new HashSet<>();

    /** Status history, oldest first. Complaints created before history existed have none. */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id")
    @OrderBy("at ASC, id ASC")
    private List<ComplaintEvent> events = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_email", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
