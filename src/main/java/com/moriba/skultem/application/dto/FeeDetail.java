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
}
