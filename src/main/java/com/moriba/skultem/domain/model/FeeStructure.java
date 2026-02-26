package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class FeeStructure extends AggregateRoot<String> {
    private String schoolId;
    private Clazz clazz;
    private Term term;
    private FeeCategory category;
    private boolean allowInstallment;
    private LocalDate dueDate;
    private AcademicYear academicYear;
    private BigDecimal amount;
    private String description;

    public FeeStructure(String id, String schoolId, Clazz clazz, Term term, FeeCategory category, AcademicYear academicYear, boolean allowInstallment, LocalDate dueDate, BigDecimal amount, String description, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.amount = amount;
        this.academicYear = academicYear;
        this.term = term;
        this.clazz = clazz;
        this.category = category;
        this.allowInstallment = allowInstallment;
        this.dueDate = dueDate;
        this.description = description;
        touch(updatedAt);
    }

    public static FeeStructure create(String id, String schoolId, Clazz clazz, Term term, FeeCategory category, AcademicYear academicYear, LocalDate dueDate, BigDecimal amount, String description, boolean allowInstallment) {
        Instant now = Instant.now();
        return new FeeStructure(id, schoolId, clazz, term, category, academicYear, allowInstallment, dueDate, amount, description, now, now);
    }
}
