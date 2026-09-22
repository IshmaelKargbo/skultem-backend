package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

// One row of the Payment History / Daily Collection transaction table. recordedBy is the acting
// user's display name, or null for a payment recorded before Payment#recordedByUserId existed.
public record FeePaymentRowDTO(
        String id,
        Instant paidAt,
        String receiptNo,
        String studentId,
        String studentName,
        String className,
        String feeCategoryName,
        BigDecimal amount,
        String method,
        String recordedBy) {
}
