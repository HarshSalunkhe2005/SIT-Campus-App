package com.sit.campusbackend.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sit.campusbackend.auth.entity.Student;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, String> {

    Optional<Student> findByEmail(String email);

    Optional<Student> findByPrn(String prn);
}