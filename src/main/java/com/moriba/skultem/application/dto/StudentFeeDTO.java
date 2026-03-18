package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record StudentFeeDTO(String id, String student, String clazz, String term, String fee, BigDecimal amount,
                BigDecimal amountPaid, BigDecimal outstanding, LocalDate dueDate, String status, Instant createdAt, Instant updatedAt) {
}
