package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.moriba.skultem.application.error.RuleException;
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
    /**
     * Set for the platform fee the system itself seeds for every school (see
     * SeedPlatformFeeForAcademicYearUseCase) - never for anything a school admin creates. Locked
     * against {@link #update} and against deletion (see DeleteFeeStructureUseCase) regardless of
     * role, since it isn't the school's fee to remove.
     */
    private boolean system;

    public enum Type {
        ALL, SELECTION, CLASS
    }

    public FeeStructure(String id, String schoolId, Type type, Clazz clazz, Term term, FeeCategory category,
            AcademicYear academicYear, boolean allowInstallment, Material material, boolean hasSuppy, int totalSupply,
            LocalDate dueDate,
            BigDecimal amount, String description, boolean system, Instant createdAt, Instant updatedAt) {
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
        this.system = system;
        touch(updatedAt);
    }

    public static FeeStructure create(String schoolId, Type type, Clazz clazz, boolean hasSuppy, int totalSupply,
            Term term, FeeCategory category, Material material, AcademicYear academicYear, LocalDate dueDate,
            BigDecimal amount, String description, boolean allowInstallment) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new FeeStructure(id, schoolId, type, clazz, term, category, academicYear, allowInstallment, material, hasSuppy,
                totalSupply, dueDate, amount, description, false, now, now);
    }

    /**
     * Creates the platform fee - see {@link #system}. Always type ALL: the platform fee applies to
     * every student, never a class or a selection.
     */
    public static FeeStructure createSystemFee(String schoolId, Term term, FeeCategory category,
            AcademicYear academicYear, LocalDate dueDate, BigDecimal amount, String description) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new FeeStructure(id, schoolId, Type.ALL, null, term, category, academicYear, false, null, false, 0,
                dueDate, amount, description, true, now, now);
    }

    /**
     * Updates this fee structure's details in place. What it applies to - academic year, class, type
     * - is deliberately not editable here: changing who a fee targets after the fact wouldn't move
     * already-created {@code StudentFee}/ledger records, so a retarget is a delete-and-recreate, not
     * an edit. The platform fee (see {@link #system}) can't be edited at all.
     */
    public void update(Term term, FeeCategory category, Material material, boolean hasSupply, int totalSupply,
            LocalDate dueDate, BigDecimal amount, String description, boolean allowInstallment) {
        if (system) {
            throw new RuleException("The platform fee can't be edited");
        }

        this.term = term;
        this.category = category;
        this.material = material;
        this.hasSupply = hasSupply;
        this.totalSupply = totalSupply;
        this.dueDate = dueDate;
        this.amount = amount;
        this.description = description;
        this.allowInstallment = allowInstallment;
        touch(Instant.now());
    }
}
