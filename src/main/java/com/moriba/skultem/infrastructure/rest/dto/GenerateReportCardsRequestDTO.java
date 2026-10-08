package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateReportCardsRequestDTO(
        @NotBlank(message = "Class is required") String classId,
        String termId,
        boolean includeAttendance,
        boolean includeRanking,
        java.util.List<String> assessmentIds,
        boolean wholeYear,
        String sectionId,
        String streamId,
        String academicYearId) {
}
