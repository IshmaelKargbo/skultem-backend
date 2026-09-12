package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record RecordSalePaymentDTO(

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @Pattern(regexp = "CASH|BANK|MOBILE_MONEY", message = "Payment method must be CASH, BANK or MOBILE_MONEY")
        String paymentMethod) {
}
