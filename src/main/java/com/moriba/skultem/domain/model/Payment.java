package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Payment extends AggregateRoot<String> {
    private String schoolId;
    private Student student;
    private FeeStructure fee;
    private PaymentMethod method;
    private String referenceNo;
    private String note;
    private BigDecimal amount;
    private Instant paidAt;

    public enum PaymentMethod {
        CASH,
        BANK,
        MOBILE_MONEY
    }

    public Payment(String id, String schoolId, Student student, FeeStructure fee, BigDecimal amount,
            PaymentMethod method,
            String referenceNo, String note, Instant paidAt, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.student = student;
        this.fee = fee;
        this.method = method;
        this.referenceNo = referenceNo;
        this.amount = amount;
        this.paidAt = paidAt;
        touch(updatedAt);
    }

    public static Payment create(String id, String schoolId, Student student, FeeStructure fee, BigDecimal amount,
            PaymentMethod method, String referenceNo, String note, Instant paidAt) {
        Instant now = Instant.now();
        return new Payment(id, schoolId, student, fee, amount, method, referenceNo, note, paidAt, now, now);
    }
}
