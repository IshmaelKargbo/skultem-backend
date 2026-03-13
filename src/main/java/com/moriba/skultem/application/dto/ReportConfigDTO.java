package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ReportConfigDTO(
        String id,
        String name,
        String type,
        String format,
        String classId,
        String classSessionId,
        String teacherSubjectId,
        String termId,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt,
        Instant updatedAt) {
}
