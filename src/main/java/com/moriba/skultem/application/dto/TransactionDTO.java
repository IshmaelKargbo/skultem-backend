package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.moriba.skultem.domain.model.Transaction.Direction;
import com.moriba.skultem.domain.model.Transaction.ReferenceType;
import com.moriba.skultem.domain.model.Transaction.TransactionType;

public record TransactionDTO(String id, String academicYearId, String termId, TransactionType type, Direction direction,
        BigDecimal amount, BigDecimal balance, String referenceId, ReferenceType referenceType, Instant createdAt) {
}
