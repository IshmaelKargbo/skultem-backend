package com.moriba.skultem.application.dto;

import java.time.Instant;

import com.moriba.skultem.domain.model.Enrollment.Status;

public record EnrollmentDTO(String id, String schoolId, AcademicYearDTO academicYear, StudentDTO student, ClassDTO clazz, StreamDTO stream, SectionDTO section, Status status, Instant createdAt, Instant updatedAt) {

}
