package com.moriba.skultem.application.dto;

import java.math.BigDecimal;

// method is Payment.PaymentMethod's name (CASH / BANK / MOBILE_MONEY) - the only methods Skultem
// actually supports; see Payment.PaymentMethod.
public record PaymentMethodAmountDTO(
        String method,
        BigDecimal amount,
        int transactionCount,
        double percentage) {
}
