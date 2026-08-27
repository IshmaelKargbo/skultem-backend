package com.moriba.skultem.application.dto;

public record GenerateReportCardsDTO(String classId, String termId, boolean includeAttendance,
        boolean includeRanking) {
}
