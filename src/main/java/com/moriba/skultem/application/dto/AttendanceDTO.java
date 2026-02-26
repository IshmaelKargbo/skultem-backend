package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AttendanceDTO(
        String id,
        String schoolId,
        EnrollmentDTO enrollment,
        LocalDate date,
        boolean present,
        boolean excused,
        boolean late,
        String reason,
        boolean holiday,
        Instant createdAt,
        Instant updatedAt) {
}
