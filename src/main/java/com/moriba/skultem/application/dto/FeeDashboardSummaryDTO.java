package com.moriba.skultem.application.dto;

import java.math.BigDecimal;

public record FeeDashboardSummaryDTO(
        String academicYearId,
        String academicYearName,
        String termId,
        String termName,
        BigDecimal totalExpected,
        BigDecimal totalCollected,
        BigDecimal totalOutstanding,
        double collectionRate,
        int totalStudents,
        int studentsFullyPaid,
        int studentsWithBalance,
        int studentsNoPayment) {
}
