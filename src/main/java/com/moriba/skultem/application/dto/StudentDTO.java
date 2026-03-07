package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.Student.Status;
import com.moriba.skultem.domain.model.vo.Gender;

public record StudentDTO(String id, String enrollmentId, String admissionNumber, String givenNames, String familyName, Gender gender,
        LocalDate dateOfBirth, String classId, String className, Status status, Instant createdAt, Instant updatedAt) {
}
