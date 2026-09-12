package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.moriba.skultem.domain.model.MaterialSale.PaymentMethod;
import com.moriba.skultem.domain.model.MaterialSale.PaymentStatus;
import com.moriba.skultem.domain.model.MaterialSale.Status;

public record MaterialSaleDTO(String id, StudentDTO student, String customerName, MaterialDTO material,
        int quantity, BigDecimal unitPrice, BigDecimal totalAmount, BigDecimal amountPaid, BigDecimal balance,
        PaymentStatus paymentStatus, PaymentMethod paymentMethod, String note, Status status, Instant fulfilledAt,
        Instant createdAt, Instant updatedAt) {
}
