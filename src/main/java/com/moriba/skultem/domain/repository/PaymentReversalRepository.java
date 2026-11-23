package com.moriba.skultem.domain.repository;

import java.time.Instant;

import com.moriba.skultem.domain.model.Payment;

// The permanent record of a payment that was reversed (see ReversePaymentUseCase).
public interface PaymentReversalRepository {

    void record(Payment payment, String reason, String reversedByUserId, Instant reversedAt);
}
