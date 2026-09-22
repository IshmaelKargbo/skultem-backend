package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record PaymentMethodSummaryDTO(
        BigDecimal totalCollected,
        List<PaymentMethodAmountDTO> methods) {
}
