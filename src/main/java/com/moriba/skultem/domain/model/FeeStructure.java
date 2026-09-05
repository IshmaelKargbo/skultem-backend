package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Gender;

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
    private Type type;
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
     * currently-enrolled roster when backfilling a fee created with this set.
     */
    private boolean newStudentsOnly;

    /**
     * True for a fee that should only be charged to students whose {@link Student#getEnrollmentType()}
     * is RE_ENROLLMENT (a returning student) - never a NEW or TRANSFER admission (e.g. a re-enrollment/
     * continuation fee that a brand-new student wouldn't owe). Mutually exclusive with
     * {@link #newStudentsOnly} - see CreateFeeStructureDTO. See ApplyApplicableFeesToEnrollmentUseCase,
     * which enforces this for enrollments created after this fee already exists, and
     * CreateFeeStructureUseCase, which applies the same filter to the currently-enrolled roster when
     * backfilling a fee created with this set.
     */
    private boolean oldStudentsOnly;

    /**
     * Null means "every gender" - set when a fee only applies to one (e.g. a boys' vs girls'
     * uniform supply fee, priced differently per gender under the same fee category/term). Same
     * apply-forward/backfill split as newStudentsOnly/oldStudentsOnly: enforced here for the
     * currently-enrolled roster at creation, and by ApplyApplicableFeesToEnrollmentUseCase for
     * every enrollment created afterwards.
     */
    private Gender gender;

    /**
     * The materials this fee bundles when {@link #hasSupply} is true - a Uniform fee might carry
     * the Uniform itself, a House Colour, and a Necktie as three separate lines, each with its own
     * quantity, instead of being limited to one material. Empty when hasSupply is false. See
     * RecordPaymentUseCase#processSupply, which issues one Supply record per line once the fee is
     * fully paid.
     */
    private List<FeeStructureSupplyItem> supplyItems;

    public enum Type {
        ALL, SELECTION, CLASS
    }

    public FeeStructure(String id, String schoolId, Type type, Clazz clazz, Term term, FeeCategory category,
            AcademicYear academicYear, boolean allowInstallment, List<FeeStructureSupplyItem> supplyItems,
            boolean hasSuppy, LocalDate dueDate,
            BigDecimal amount, String description, boolean system, boolean newStudentsOnly, boolean oldStudentsOnly,
            Gender gender, Instant createdAt, Instant updatedAt) {
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
        this.supplyItems = supplyItems;
        this.dueDate = dueDate;
        this.description = description;
        this.system = system;
        this.newStudentsOnly = newStudentsOnly;
        this.oldStudentsOnly = oldStudentsOnly;
        this.gender = gender;
        touch(updatedAt);
    }

    public static FeeStructure create(String schoolId, Type type, Clazz clazz, boolean hasSuppy,
            List<FeeStructureSupplyItem> supplyItems, Term term, FeeCategory category, AcademicYear academicYear,
            LocalDate dueDate, BigDecimal amount, String description, boolean allowInstallment,
            boolean newStudentsOnly, boolean oldStudentsOnly, Gender gender) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new FeeStructure(id, schoolId, type, clazz, term, category, academicYear, allowInstallment,
                supplyItems, hasSuppy, dueDate, amount, description, false, newStudentsOnly, oldStudentsOnly, gender,
                now, now);
    }

    /**
     * Creates the platform fee - see {@link #system}. Always type ALL: the platform fee applies to
     * every student, never a class or a selection.
     */
    public static FeeStructure createSystemFee(String schoolId, Term term, FeeCategory category,
            AcademicYear academicYear, LocalDate dueDate, BigDecimal amount, String description) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new FeeStructure(id, schoolId, Type.ALL, null, term, category, academicYear, false, List.of(), false,
                dueDate, amount, description, true, false, false, null, now, now);
    }

    /**
     * Updates this fee structure's details in place. What it applies to - academic year, class, type
     * - is deliberately not editable here: changing who a fee targets after the fact wouldn't move
     * already-created {@code StudentFee}/ledger records, so a retarget is a delete-and-recreate, not
     * an edit. The platform fee (see {@link #system}) can't be edited at all.
     *
     * newStudentsOnly/oldStudentsOnly/gender ARE editable here, unlike the rest of what a fee
     * targets - same caveat as everywhere else in this app that a "structure" edit only reaches
     * forward (SalaryStructure, curriculum templates, ...): ApplyApplicableFeesToEnrollmentUseCase
     * reads these for enrollments created from this point on, but this does not retroactively add
     * or remove the StudentFee rows already created for students currently enrolled - see
     * UpdateFeeStructureUseCase's caller-facing note on this. supplyItems are editable the same
     * way - a student who already had supply issued from the old line-up keeps it; only students
     * who pay after this edit see the new list.
     */
    public void update(Term term, FeeCategory category, List<FeeStructureSupplyItem> supplyItems, boolean hasSupply,
            LocalDate dueDate, BigDecimal amount, String description, boolean allowInstallment,
            boolean newStudentsOnly, boolean oldStudentsOnly, Gender gender) {
        if (system) {
            throw new RuleException("The platform fee can't be edited");
        }

        if (newStudentsOnly && oldStudentsOnly) {
            throw new RuleException("newStudentsOnly and oldStudentsOnly cannot both be true");
        }

        if ((newStudentsOnly || oldStudentsOnly || gender != null) && type == Type.SELECTION) {
            throw new RuleException("newStudentsOnly/oldStudentsOnly/gender are not allowed for a selection-based fee");
        }

        this.term = term;
        this.category = category;
        this.supplyItems = supplyItems;
        this.hasSupply = hasSupply;
        this.dueDate = dueDate;
        this.amount = amount;
        this.description = description;
        this.allowInstallment = allowInstallment;
        this.newStudentsOnly = newStudentsOnly;
        this.oldStudentsOnly = oldStudentsOnly;
        this.gender = gender;
        touch(Instant.now());
    }
}
