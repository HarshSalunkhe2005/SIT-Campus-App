package com.sit.campusbackend.complaint.repository;

import com.sit.campusbackend.complaint.entity.Complaint;
import com.sit.campusbackend.complaint.entity.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStudentEmail(String email);

    List<Complaint> findByDepartmentId(Long departmentId);

    List<Complaint> findByStatus(ComplaintStatus status);

    long countByStatus(ComplaintStatus status);
}
