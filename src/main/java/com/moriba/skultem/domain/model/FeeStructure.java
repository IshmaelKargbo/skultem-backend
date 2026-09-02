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

    /**
     * True for a fee that should only be charged to students whose {@link Student#getEnrollmentType()}
     * is NEW or TRANSFER (e.g. a Uniform Fee) - never a RE_ENROLLMENT (a returning student). See
     * ApplyApplicableFeesToEnrollmentUseCase, which enforces this for enrollments created after
     * this fee already exists, and CreateFeeStructureUseCase, which applies the same filter to the
     * currently-enrolled roster when backfilling a fee created with this set. Not editable after
     * creation, same as the rest of what a fee targets - see {@link #update}.
     */
    private boolean newStudentsOnly;

    public enum Type {
        ALL, SELECTION, CLASS
    }

    public FeeStructure(String id, String schoolId, Type type, Clazz clazz, Term term, FeeCategory category,
            AcademicYear academicYear, boolean allowInstallment, Material material, boolean hasSuppy, int totalSupply,
            LocalDate dueDate,
            BigDecimal amount, String description, boolean system, boolean newStudentsOnly, Instant createdAt,
            Instant updatedAt) {
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
        this.newStudentsOnly = newStudentsOnly;
        touch(updatedAt);
    }

    public static FeeStructure create(String schoolId, Type type, Clazz clazz, boolean hasSuppy, int totalSupply,
            Term term, FeeCategory category, Material material, AcademicYear academicYear, LocalDate dueDate,
            BigDecimal amount, String description, boolean allowInstallment, boolean newStudentsOnly) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new FeeStructure(id, schoolId, type, clazz, term, category, academicYear, allowInstallment, material, hasSuppy,
                totalSupply, dueDate, amount, description, false, newStudentsOnly, now, now);
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
                dueDate, amount, description, true, false, now, now);
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
