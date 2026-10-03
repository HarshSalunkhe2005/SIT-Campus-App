package com.sit.campusbackend.auth.repository;

import com.sit.campusbackend.auth.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, String> {

    boolean existsByEmailAndIsVerifiedTrue(String email);
}
