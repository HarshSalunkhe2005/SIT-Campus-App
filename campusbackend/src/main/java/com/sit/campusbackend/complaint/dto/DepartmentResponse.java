package com.sit.campusbackend.complaint.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** {@code initialPassword} is only present once, in the response to creating a department with a generated password. */
public record DepartmentResponse(
    Long id, String name, String type, String email,
    @JsonInclude(JsonInclude.Include.NON_NULL) String initialPassword
) {}
