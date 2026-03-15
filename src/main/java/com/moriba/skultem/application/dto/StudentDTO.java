package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.Student.Status;
import com.moriba.skultem.domain.vo.Gender;

public record StudentDTO(String id, String enrollmentId, String admissionNumber, String givenNames, String familyName, Gender gender,
        LocalDate dateOfBirth, Integer age, String classId, String className, String guardianName, String fatherName, String motherName, Status status, Instant createdAt, Instant updatedAt) {
}
