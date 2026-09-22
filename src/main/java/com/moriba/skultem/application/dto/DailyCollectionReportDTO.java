package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailyCollectionReportDTO(
        LocalDate from,
        LocalDate to,
        BigDecimal totalCollected,
        int paymentCount,
        List<PaymentMethodAmountDTO> byMethod) {
}
