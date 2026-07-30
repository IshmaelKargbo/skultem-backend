package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AttendanceDTO(
        String id,
        String studentId,
        String student,
        String photo,
        String clazz,
        LocalDate date,
        String state,
        String reason,
        Instant createdAt,
        Instant updatedAt) {
}
