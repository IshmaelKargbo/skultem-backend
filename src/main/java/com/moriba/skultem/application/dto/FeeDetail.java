package com.moriba.skultem.application.dto;

import java.math.BigDecimal;

// total is the full amount charged; discount is what discounts take off it, so what is actually owed is
// total - discount. balance and status are already worked out net of the discount.
public record FeeDetail(
        BigDecimal total,
        BigDecimal paid,
        BigDecimal balance,
        String status,
        BigDecimal discount
) {
    public FeeDetail(BigDecimal total, BigDecimal paid, BigDecimal balance, String status) {
        this(total, paid, balance, status, BigDecimal.ZERO);
    }

    /**
     * Totals for a group of students (a class, a parent's children), with the status worked out from those totals:
     * Paid when nothing is owed, Partial when something has been paid but a balance remains, Pending when nothing
     * has been paid. Never one student's status standing in for the whole group.
     */
    public static FeeDetail combined(BigDecimal total, BigDecimal paid, BigDecimal balance) {
        String status;
        if (balance == null || balance.signum() <= 0) {
            status = "Paid";
        } else if (paid != null && paid.signum() > 0) {
            status = "Partial";
        } else {
            status = "Pending";
        }
        return new FeeDetail(total, paid, balance, status);
    }
}
