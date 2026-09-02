package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SetSalaryStructureDTO(

        @NotBlank(message = "Teacher is required")
        String teacherId,

        @NotNull(message = "Basic salary is required")
        @DecimalMin(value = "0", message = "Basic salary cannot be negative")
        BigDecimal basicSalary,

        @NotNull(message = "Allowances is required")
        @DecimalMin(value = "0", message = "Allowances cannot be negative")
        BigDecimal allowances,

        @NotNull(message = "Deductions is required")
        @DecimalMin(value = "0", message = "Deductions cannot be negative")
        BigDecimal deductions

) {
}
