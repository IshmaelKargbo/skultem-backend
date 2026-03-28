package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Transaction extends AggregateRoot<String> {

    private final String schoolId;
    private final String academicYearId;
    private final String termId;

    private final TransactionType type;
    private final Direction direction;

    private final BigDecimal amount;
    private final BigDecimal balance;

    private final String referenceId;
    private final ReferenceType referenceType;

    public enum TransactionType {
        FEE_ASSIGNMENT,
        PAYMENT,
        DISCOUNT,
        REFUND,
        EXPENSE,
        ADJUSTMENT
    }

    public enum Direction {
        DEBIT,   // money out
        CREDIT   // money in
    }

    public enum ReferenceType {
        STUDENT,
        EXPENSE,
        SYSTEM,
        OTHER
    }

    public Transaction(
            String id,
            String schoolId,
            String academicYearId,
            String termId,
            TransactionType type,
            Direction direction,
            BigDecimal amount,
            BigDecimal balance,
            String referenceId,
            ReferenceType referenceType,
            Instant createdAt
    ) {
        super(id, createdAt);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        this.schoolId = schoolId;
        this.academicYearId = academicYearId;
        this.termId = termId;

        this.type = type;
        this.direction = direction;

        this.amount = amount;
        this.balance = balance;

        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }

    public static Transaction create(
            String id,
            String schoolId,
            String academicYearId,
            String termId,
            TransactionType type,
            Direction direction,
            BigDecimal amount,
            BigDecimal balance,
            String referenceId,
            ReferenceType referenceType
    ) {

        return new Transaction(
                id,
                schoolId,
                academicYearId,
                termId,
                type,
                direction,
                amount,
                balance,
                referenceId,
                referenceType,
                Instant.now()
        );
    }

    public BigDecimal getDebit() {
        return direction == Direction.DEBIT ? amount : BigDecimal.ZERO;
    }

    public BigDecimal getCredit() {
        return direction == Direction.CREDIT ? amount : BigDecimal.ZERO;
    }

}