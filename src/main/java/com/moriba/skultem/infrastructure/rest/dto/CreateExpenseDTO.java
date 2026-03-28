package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;

public record CreateExpenseDTO(
        @NotBlank(message = "Expense category is required")
        String category,

        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Expense date is required")
        @FutureOrPresent(message = "Expense date cannot be in the past")
        LocalDate expenseDate,

        @Length(max = 255, message = "Description must not exceed 255 characters")
        String description
) {
}