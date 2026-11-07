package com.moriba.skultem.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

// Where one part of a student's installment plan stands. amount is that part of what the student owes after
// discounts; paid is how much of it their payments cover so far (payments fill the parts in order).
public record InstallmentStatus(String label, BigDecimal amount, BigDecimal paid, LocalDate dueDate, State state) {

    public enum State {
        PAID,
        PARTIAL,
        DUE,
        OVERDUE
    }

    public BigDecimal remaining() {
        return amount.subtract(paid).max(BigDecimal.ZERO);
    }
}
