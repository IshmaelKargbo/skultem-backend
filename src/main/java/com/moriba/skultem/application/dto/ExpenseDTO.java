package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseDTO(String id, String title, String category, BigDecimal amount, LocalDate expenseDate,
        String description, Instant createdAt, Instant updatedAt) {
}
