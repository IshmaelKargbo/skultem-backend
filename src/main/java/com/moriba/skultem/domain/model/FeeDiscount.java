package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.utils.Formate;
import com.moriba.skultem.utils.MoneyUtil;

import lombok.Getter;

@Getter
public class FeeDiscount extends AggregateRoot<String> {
    private String schoolId;
    private String name;
    private Kind kind;
    private FeeStructure fee;
    private Enrollment enrollment;
    private BigDecimal value;
    private LocalDate expiryDate;
    private String reason;

    public enum Kind {
        PERCENTAGE,
        AMOUNT
    }

    public FeeDiscount(String id, String schoolId, String name, Kind kind, BigDecimal value, Enrollment enrollment,
            FeeStructure fee, String reason, LocalDate expiryDate, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.fee = fee;
        this.kind = kind;
        this.expiryDate = expiryDate;
        this.value = value;
        this.enrollment = enrollment;
        this.reason = reason;
        touch(updatedAt);
    }

    public static FeeDiscount create(String id, String schoolId, String name, Kind kind, BigDecimal value,
            Student assignTo, LocalDate expiryDate, Enrollment enrollment, FeeStructure fee, String reason) {
        Instant now = Instant.now();
        return new FeeDiscount(id, schoolId, name, kind, value, enrollment, fee, reason, expiryDate, now, now);
    }

    public boolean isActive() {
        return expiryDate == null || !expiryDate.isBefore(LocalDate.now());
    }

    public String value() {
        switch (kind) {
            case AMOUNT -> {
                return MoneyUtil.format(value);
            }
            case PERCENTAGE -> {
                return Formate.formatMoney(value) + "%";
            }
            default -> {
                return "";
            }
        }
    }

    public BigDecimal computeSavings() {
        if (fee == null || value == null) {
            return BigDecimal.ZERO;
        }

        switch (kind) {
            case AMOUNT -> {
                return value;
            }
            case PERCENTAGE -> {
                BigDecimal feeAmount = fee.getAmount();
                return feeAmount.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            default -> {
                return BigDecimal.ZERO;
            }
        }
    }

    public LocalDate appliedDate() {
        return LocalDate.ofInstant(this.createdAt, ZoneId.systemDefault());
    }

    public String getStudent() {
        var student = enrollment.getStudent();
        return String.join("", student.getGivenNames(), student.getFamilyName());
    }

    public String getClazz() {
        return enrollment.getClazz().getName();
    }
}
