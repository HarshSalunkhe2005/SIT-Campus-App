package com.sit.campusbackend.complaint.repository;

import com.sit.campusbackend.complaint.entity.Complaint;
import com.sit.campusbackend.complaint.entity.ComplaintEvent;
import com.sit.campusbackend.complaint.entity.ComplaintStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** The list queries fetch student and department in the same query so mapping them to responses is not N+1. */
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    @EntityGraph(attributePaths = {"student", "department"})
    List<Complaint> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"student", "department"})
    List<Complaint> findByStudentEmailOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"student", "department"})
    List<Complaint> findByDepartmentIdOrderByCreatedAtDesc(Long departmentId);

    @Query("select c.status, count(c) from Complaint c group by c.status")
    List<Object[]> countGroupedByStatus();

    /** History for a whole list of complaints in one query. */
    @Query("select e from ComplaintEvent e where e.complaintId in :ids order by e.at, e.id")
    List<ComplaintEvent> findEventsForComplaints(@Param("ids") Collection<Long> ids);

    void deleteByStudentEmail(String email);

    void deleteByDepartmentId(Long departmentId);
}
