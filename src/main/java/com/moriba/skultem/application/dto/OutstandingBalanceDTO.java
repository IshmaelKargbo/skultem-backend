package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OutstandingBalanceDTO(String feeId, String feeName, BigDecimal total, BigDecimal paid,
        BigDecimal outstanding, BigDecimal discount, LocalDate dueDate, String status, String term,
        boolean allowInstallment,
        // Each part of the fee's installment plan and where the student stands on it (empty = no plan).
        java.util.List<com.moriba.skultem.domain.vo.InstallmentStatus> installments) {
}
