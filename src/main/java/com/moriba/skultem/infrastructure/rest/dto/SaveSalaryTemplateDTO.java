package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Shared by create and update - a template has no state machine to protect (unlike a
// SalaryStructure once it's been used in a payroll run), so both endpoints accept the same shape.
public record SaveSalaryTemplateDTO(

        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Basic salary is required")
        @DecimalMin(value = "0", message = "Basic salary cannot be negative")
        BigDecimal basicSalary,

        @Valid
        List<PayComponentItemDTO> allowances,

        @Valid
        List<PayComponentItemDTO> deductions

) {
}
