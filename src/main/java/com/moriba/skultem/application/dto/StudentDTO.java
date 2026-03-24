package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.Student.Status;
import com.moriba.skultem.domain.vo.Gender;

public record StudentDTO(String id, String enrollmentId, String admissionNumber, String givenNames, String familyName,
        Gender gender, LocalDate dateOfBirth, Integer age, String classId, int classSize, String className,
        String classTeacher, String guardianName, String fatherName, String motherName, String sessionId, Status status, FeeDetail feeDetail,
        Instant createdAt, Instant updatedAt) {
}
