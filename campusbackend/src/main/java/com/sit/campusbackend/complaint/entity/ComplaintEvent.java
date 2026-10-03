package com.sit.campusbackend.complaint.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** One step in a complaint's history: it entered {@code status} at {@code at}. */
@Entity
@Table(name = "complaint_events", indexes = @Index(name = "idx_complaint_events_complaint", columnList = "complaint_id"))
@Getter
@Setter
public class ComplaintEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The join column is owned by Complaint.events; this read-only copy exists for queries. */
    @Column(name = "complaint_id", insertable = false, updatable = false)
    private Long complaintId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status;

    @Column(nullable = false)
    private LocalDateTime at;

    protected ComplaintEvent() {
    }

    public ComplaintEvent(ComplaintStatus status, LocalDateTime at) {
        this.status = status;
        this.at = at;
    }
}
