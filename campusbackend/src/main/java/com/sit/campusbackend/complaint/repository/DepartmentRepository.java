package com.sit.campusbackend.complaint.repository;

import com.sit.campusbackend.complaint.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByType(String type);

    Optional<Department> findByEmail(String email);
}
