package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AttendanceHistoryDTO(
                LocalDate date,
                String classId,
                String className,
                Long presentCount,
                Long totalCount,
                Instant createdAt,
                Instant updatedAt
                ) {
}