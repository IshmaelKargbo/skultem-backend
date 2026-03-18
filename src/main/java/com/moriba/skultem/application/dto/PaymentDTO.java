package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.moriba.skultem.domain.model.Payment.PaymentMethod;

public record PaymentDTO(String id, String student, String fee, BigDecimal amount, Instant paidAt, PaymentMethod paymentMethod, String referenceNo, String description, Instant createdAt, Instant updatedAt) {
}
