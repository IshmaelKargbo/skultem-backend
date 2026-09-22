package com.moriba.skultem.application.dto;

import java.math.BigDecimal;

// One row of the Student Balances report. status is FeeCollectionCalculator.Status's name
// (PAID / PARTIALLY_PAID / NO_PAYMENT) - the frontend maps it to a label/color the same way it
// already does for other raw status strings.
public record StudentFeeBalanceDTO(
        String studentId,
        String admissionNumber,
        String studentName,
        String className,
        BigDecimal expected,
        BigDecimal paid,
        BigDecimal balance,
        String status) {
}
