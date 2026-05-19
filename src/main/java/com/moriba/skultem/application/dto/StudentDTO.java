package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.Student.Status;
import com.moriba.skultem.domain.vo.Family;
import com.moriba.skultem.domain.vo.Gender;

public record StudentDTO(String id, String photo, String enrollmentId, String admissionNumber, String givenNames,
                String familyName, Gender gender, LocalDate dateOfBirth, Integer age, String classId, int classSize,
                String className, String classTeacher, String nationality, String religion, String city, String street,
                Family family, ParentDTO guardian, String relationship, String sessionId, Status status,
                FeeDetail feeDetail, Instant createdAt, Instant updatedAt) {
}
