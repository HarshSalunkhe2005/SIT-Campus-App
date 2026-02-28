package com.sit.campusbackend.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "students")
@Data
public class Student {

    @Id
    private String email;

    private String prn;

    private String passwordHash;

    private String firstName;

    private String lastName;

    private Integer batchYear;

    private Boolean isVerified;

    private String otp;

        public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}