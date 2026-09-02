package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PayslipDTO(
        String id,
        String schoolId,
        String payrollRunId,
        String payrollRunPeriod,
        TeacherDTO teacher,
        BigDecimal basicSalary,
        BigDecimal allowances,
        BigDecimal deductions,
        BigDecimal grossSalary,
        BigDecimal netSalary,
        boolean included,
        Instant createdAt,
        Instant updatedAt) {
}
