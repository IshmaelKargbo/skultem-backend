package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record FeeTermSummaryDTO(
        BigDecimal totalExpected,
        BigDecimal totalCollected,
        BigDecimal totalOutstanding,
        double collectionRate,
        int totalStudents,
        int fullyPaidStudents,
        int studentsWithBalance,
        List<ClassFeeSummaryRowDTO> classes) {
}
