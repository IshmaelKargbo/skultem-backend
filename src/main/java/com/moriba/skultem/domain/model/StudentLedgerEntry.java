package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class StudentLedgerEntry extends AggregateRoot<String> {
    private String schoolId;
    private String studentId;
    private String termId;
    private String academicYearId;
    private TransactionType transactionType;
    private Direction direction;
    private BigDecimal amount;
    private String referenceId;
    private String description;
    private BigDecimal balance;
    private Instant paidAt;

    public enum TransactionType {
        FEE_ASSINMENT, PAYMENT, DISCOUNT, REFUND, ADJUSTMENT
    }

    public enum Direction {
        DEBIT, CREDIT
    }

    public StudentLedgerEntry(String id, String schoolId, String academicYearId, String studentId, String termId,
            TransactionType transactionType, Direction direction, BigDecimal amount, String referenceId,
            String description, Instant paidAt, BigDecimal balance, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.amount = amount;
        this.balance = balance;
        this.termId = termId;
        this.direction = direction;
        this.studentId = studentId;
        this.academicYearId = academicYearId;
        this.referenceId = referenceId;
        this.transactionType = transactionType;
        this.description = description;
        this.paidAt = paidAt;
        touch(updatedAt);
    }

    public static StudentLedgerEntry create(String id, String schoolId, String academicYearId, String studentId,
            String termId, TransactionType transactionType, Direction direction, BigDecimal amount, String referenceId,
            String description, Instant paidAt, BigDecimal balance) {
        Instant now = Instant.now();
        return new StudentLedgerEntry(id, schoolId, academicYearId, studentId, termId, transactionType, direction,
                amount, referenceId, description, paidAt, balance, now, now);
    }

    public LocalDate getDate() {
        return LocalDate.ofInstant(this.paidAt, ZoneId.systemDefault());
    }

    public BigDecimal getDebit() {
        return (direction == Direction.DEBIT && amount != null) ? amount : BigDecimal.ZERO;
    }

    public BigDecimal getCredit() {
        return (direction == Direction.CREDIT && amount != null) ? amount : BigDecimal.ZERO;
    }

    /**
     * Corrects this entry's stored running balance - used only to backfill entries written while the
     * running-balance calculation was scoped to the wrong student (see
     * {@link com.moriba.skultem.application.usecase.RecomputeStudentLedgerBalancesUseCase}). Not part
     * of normal ledger writing, where the balance is fixed at creation time.
     */
    public void recalculateBalance(BigDecimal balance) {
        this.balance = balance;
        touch(Instant.now());
    }
}
