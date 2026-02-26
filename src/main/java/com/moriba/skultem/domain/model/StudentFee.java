package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class StudentFee extends AggregateRoot<String> {
    private String schoolId;
    private Student student;
    private Enrollment enrollment;
    private FeeDiscount discount;
    private FeeStructure fee;

    public StudentFee(String id, String schoolId, Enrollment enrollment, Student student, FeeStructure fee,
            FeeDiscount discount, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.enrollment = enrollment;
        this.student = student;
        this.fee = fee;
        this.discount = discount;
        touch(updatedAt);
    }

    public static StudentFee create(String id, String schoolId, Enrollment enrollment, Student student,
            FeeStructure fee, FeeDiscount discount) {
        Instant now = Instant.now();
        return new StudentFee(id, schoolId, enrollment, student, fee, discount, now, now);
    }
}
