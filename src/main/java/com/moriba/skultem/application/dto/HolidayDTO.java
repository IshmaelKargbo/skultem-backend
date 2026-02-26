package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.Holiday.Kind;

public record HolidayDTO(
        String id,
        String schoolId,
        String name,
        Kind kind,
        LocalDate date,
        AcademicYearDTO academicYear,
        boolean fixed,
        Instant createdAt,
        Instant updatedAt) {
}
