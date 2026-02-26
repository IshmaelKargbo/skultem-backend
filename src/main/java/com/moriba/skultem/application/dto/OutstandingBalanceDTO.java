package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OutstandingBalanceDTO(String feeId, String feeName, BigDecimal total, BigDecimal paid,
        BigDecimal outstanding, BigDecimal discount, LocalDate dueDate, String status, String term) {
}
