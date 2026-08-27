package com.moriba.skultem.application.dto;

import java.math.BigDecimal;

public record PromotionProgressDTO(
        String academicYearName,
        int totalSessions,
        int completedSessions,
        int pendingSessions,
        boolean readyToCloseYear,
        boolean nextAcademicYearConfigured,
        String nextAcademicYearName,
        BigDecimal outstandingPlatformFee) {
}
