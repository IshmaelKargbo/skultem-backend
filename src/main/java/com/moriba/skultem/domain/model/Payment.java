package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Payment extends AggregateRoot<String> {
    private String schoolId;
    private Student student;
    private FeeStructure fee;
    private PaymentMethod method;
    private String referenceNo;
    private String externalReference;
    private String note;
    private BigDecimal amount;
    private Instant paidAt;
    // Who recorded this payment (the acting user at the time) - null for a payment recorded before
    // this was introduced. See RecordPaymentUseCase, which sets it from the authenticated principal,
    // same pattern as Attendance/TeacherAttendance's recordedByUserId.
    private String recordedByUserId;

    public enum PaymentMethod {
        CASH,
        BANK,
        MOBILE_MONEY
    }

    public Payment(String id, String schoolId, Student student, FeeStructure fee, BigDecimal amount,
            PaymentMethod method,
            String referenceNo, String externalReference, String note, Instant paidAt, String recordedByUserId,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.student = student;
        this.fee = fee;
        this.method = method;
        this.referenceNo = referenceNo;
        this.externalReference = externalReference;
        this.note = note;
        this.amount = amount;
        this.paidAt = paidAt;
        this.recordedByUserId = recordedByUserId;
        touch(updatedAt);
    }

    // Used when a student's class is corrected: the money already received stays (same receipt,
    // method and date) but is applied to the equivalent fee of the new class instead.
    public void reassign(FeeStructure fee, BigDecimal amount) {
        this.fee = fee;
        this.amount = amount;
        touch(Instant.now());
    }

    public static Payment create(String schoolId, Student student, FeeStructure fee, BigDecimal amount,
            PaymentMethod method, String referenceNo, String externalReference, String note, Instant paidAt,
            String recordedByUserId) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new Payment(id, schoolId, student, fee, amount, method, referenceNo, externalReference, note, paidAt,
                recordedByUserId, now, now);
    }
}
