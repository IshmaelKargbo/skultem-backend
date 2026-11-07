package com.moriba.skultem.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

// One part of a fee's installment plan: e.g. "First installment", 500.00, due 2026-10-15.
public record FeeInstallment(String label, BigDecimal amount, LocalDate dueDate) {
}
