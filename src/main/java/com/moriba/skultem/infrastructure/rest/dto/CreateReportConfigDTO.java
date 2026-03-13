package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public record CreateReportConfigDTO(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Report type is required") String type,
        @NotBlank(message = "Format is required") String format,
        String classId,
        String classSessionId,
        String teacherSubjectId,
        String termId,
        LocalDate startDate,
        LocalDate endDate) {
}
