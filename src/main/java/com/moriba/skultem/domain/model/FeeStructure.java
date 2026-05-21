package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

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
    private boolean hasSupply;
    private Material material;
    private Type type;
    private int totalSupply;
    private BigDecimal amount;
    private String description;

    public enum Type {
        ALL, SELECTION, CLASS
    }

    public FeeStructure(String id, String schoolId, Type type, Clazz clazz, Term term, FeeCategory category,
            AcademicYear academicYear, boolean allowInstallment, Material material, boolean hasSuppy, int totalSupply,
            LocalDate dueDate,
            BigDecimal amount, String description, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.amount = amount;
        this.academicYear = academicYear;
        this.term = term;
        this.type = type;
        this.clazz = clazz;
        this.category = category;
        this.allowInstallment = allowInstallment;
        this.hasSupply = hasSuppy;
        this.material = material;
        this.totalSupply = totalSupply;
        this.dueDate = dueDate;
        this.description = description;
        touch(updatedAt);
    }

    public static FeeStructure create(String schoolId, Type type, Clazz clazz, boolean hasSuppy, int totalSupply,
            Term term, FeeCategory category, Material material, AcademicYear academicYear, LocalDate dueDate,
            BigDecimal amount, String description, boolean allowInstallment) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new FeeStructure(id, schoolId, type, clazz, term, category, academicYear, allowInstallment, material, hasSuppy,
                totalSupply, dueDate, amount, description, now, now);
    }
}
