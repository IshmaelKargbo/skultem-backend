package com.moriba.skultem.application.dto;

import java.math.BigDecimal;

public record PayrollSummaryDTO(
        long teacherCount,
        BigDecimal totalGross,
        BigDecimal averageSalary,
        BigDecimal highestSalary,
        BigDecimal lowestSalary,
        PayrollRunDTO latestRun) {
}
