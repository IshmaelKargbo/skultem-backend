package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ExpenseDTO(String id, String title, String category, BigDecimal amount,
                String description, Instant createdAt, Instant updatedAt) {
}
