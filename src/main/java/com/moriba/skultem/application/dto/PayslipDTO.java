package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PayslipDTO(
        String id,
        String schoolId,
        String payrollRunId,
        String payrollRunPeriod,
        java.time.LocalDate payDate,
        TeacherDTO teacher,
        BigDecimal basicSalary,
        BigDecimal allowances,
        BigDecimal deductions,
        BigDecimal grossSalary,
        BigDecimal netSalary,
        boolean included,
        java.util.List<PayslipLineDTO> allowanceLines,
        java.util.List<PayslipLineDTO> deductionLines,
        Instant createdAt,
        Instant updatedAt) {
}
