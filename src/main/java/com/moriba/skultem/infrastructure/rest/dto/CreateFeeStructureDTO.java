package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;

public record CreateFeeStructureDTO(
        String classId,

        @NotBlank(message = "Fee category is required")
        String feeCategory,

        @NotBlank(message = "Term is required")
        String termId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Due date is required")
        @FutureOrPresent(message = "Due date cannot be in the past")
        LocalDate dueDate,

        boolean allowInstallment,

        @Length(max = 255, message = "Description must not exceed 255 characters")
        String description
) {
}