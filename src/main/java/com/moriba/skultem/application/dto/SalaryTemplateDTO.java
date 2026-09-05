package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SalaryTemplateDTO(
        String id,
        String schoolId,
        String name,
        BigDecimal basicSalary,
        List<PayComponentDTO> allowances,
        List<PayComponentDTO> deductions,
        BigDecimal totalAllowances,
        BigDecimal totalDeductions,
        BigDecimal grossSalary,
        BigDecimal netSalary,
        Instant createdAt,
        Instant updatedAt) {
}
