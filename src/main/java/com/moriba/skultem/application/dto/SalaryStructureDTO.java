package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SalaryStructureDTO(
        String id,
        String schoolId,
        TeacherDTO teacher,
        BigDecimal basicSalary,
        BigDecimal allowances,
        BigDecimal deductions,
        BigDecimal grossSalary,
        BigDecimal netSalary,
        Instant createdAt,
        Instant updatedAt) {
}
