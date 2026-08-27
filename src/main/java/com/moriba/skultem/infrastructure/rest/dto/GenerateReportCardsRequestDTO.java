package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateReportCardsRequestDTO(
        @NotBlank(message = "Class is required") String classId,
        @NotBlank(message = "Term is required") String termId,
        boolean includeAttendance,
        boolean includeRanking) {
}
