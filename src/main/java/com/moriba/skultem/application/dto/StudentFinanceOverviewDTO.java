package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record StudentFinanceOverviewDTO(
        StudentDTO student,
        Double assignedFeeTotal,
        BigDecimal totalPaid,
        BigDecimal totalDiscount,
        BigDecimal totalOutstanding,
        List<OutstandingBalanceDTO> feeBreakdown,
        List<PaymentDTO> recentPayments) {
}
