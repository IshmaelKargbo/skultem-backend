package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// One allowance/deduction line as submitted from the client - either building a SalaryTemplate or
// setting a teacher's SalaryStructure directly, with or without a template. type is a plain string
// here (validated against the two known values) rather than the domain enum, same as every other
// request DTO in this app that carries an enum-shaped field across the wire.
public record PayComponentItemDTO(

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Type is required")
        @Pattern(regexp = "FIXED|PERCENTAGE", message = "Type must be FIXED or PERCENTAGE")
        String type,

        @NotNull(message = "Value is required")
        @DecimalMin(value = "0", message = "Value cannot be negative")
        BigDecimal value

) {
}
