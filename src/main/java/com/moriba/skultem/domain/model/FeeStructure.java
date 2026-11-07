package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.FeeInstallment;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.domain.vo.InstallmentStatus;

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
     * Null reaches every stream of the class. Set on a CLASS fee for a streamed class to charge only
     * that stream's students (e.g. SSS 1 Science priced differently from SSS 1 Art) - a partition
     * like gender: a Science fee and an Art fee for the same class/term/category coexist. Like the
     * class itself, not editable after creation (see {@link #update}).
     */
    private Stream stream;

    /**
     * The materials this fee bundles when {@link #hasSupply} is true - a Uniform fee might carry
     * the Uniform itself, a House Colour, and a Necktie as three separate lines, each with its own
     * quantity, instead of being limited to one material. Empty when hasSupply is false. See
     * RecordPaymentUseCase#processSupply, which issues one Supply record per line once the fee is
     * fully paid.
     */
    private List<FeeStructureSupplyItem> supplyItems;

    /**
     * The installment plan, when {@link #allowInstallment} is on and the school set one: two or more parts adding
     * up to {@link #amount}, in due-date order. Empty = no plan (the whole fee is due by {@link #dueDate}, and when
     * installments are allowed any part payment is accepted). Payments go against the fee as a whole; the plan only
     * decides how much should be paid by when - see {@link #isOverdue} and {@link #installmentStatuses}.
     */
    private List<FeeInstallment> installments = new ArrayList<>();

    public enum Type {
        ALL, SELECTION, CLASS
    }

    public FeeStructure(String id, String schoolId, Type type, Clazz clazz, Term term, FeeCategory category,
            AcademicYear academicYear, boolean allowInstallment, List<FeeStructureSupplyItem> supplyItems,
            boolean hasSuppy, LocalDate dueDate,
            BigDecimal amount, String description, boolean system, boolean newStudentsOnly, boolean oldStudentsOnly,
            Gender gender, Stream stream, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.stream = stream;
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
            boolean newStudentsOnly, boolean oldStudentsOnly, Gender gender, Stream stream) {
        if (stream != null && (type != Type.CLASS || clazz == null)) {
            throw new RuleException("A stream can only be set on a class fee");
        }
        if (stream != null && !clazz.getLevel().isStreamed()) {
            throw new RuleException(clazz.getName() + " has no streams - leave the stream empty for it");
        }
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new FeeStructure(id, schoolId, type, clazz, term, category, academicYear, allowInstallment,
                supplyItems, hasSuppy, dueDate, amount, description, false, newStudentsOnly, oldStudentsOnly, gender,
                stream, now, now);
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
                dueDate, amount, description, true, false, false, null, null, now, now);
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
        if (!allowInstallment) {
            // No installments, no plan.
            this.installments = new ArrayList<>();
        }
        this.newStudentsOnly = newStudentsOnly;
        this.oldStudentsOnly = oldStudentsOnly;
        this.gender = gender;
        touch(Instant.now());
    }

    /** Loads a saved plan without validating or touching updatedAt - for the persistence mapper only. */
    public FeeStructure restoreInstallments(List<FeeInstallment> saved) {
        this.installments = saved == null ? new ArrayList<>() : new ArrayList<>(saved);
        return this;
    }

    /**
     * Sets (or, with an empty list, clears) the installment plan. A plan needs installments allowed, at least two
     * parts, each above zero and with a due date, in date order, adding up to the fee. The fee's own due date
     * becomes the last part's. The platform fee's plan is set by {@link #applyPlatformPlan} instead.
     */
    public void setInstallmentPlan(List<FeeInstallment> plan) {
        if (system) {
            throw new RuleException("The platform fee's plan is set by Skultem");
        }
        applyPlan(plan);
    }

    /** The platform fee's plan (see PlatformFeeSetting): by term, or one payment when the plan is empty. */
    public void applyPlatformPlan(List<FeeInstallment> plan) {
        if (!system) {
            throw new RuleException("Only the platform fee takes a platform plan");
        }
        this.allowInstallment = plan != null && !plan.isEmpty();
        applyPlan(plan);
    }

    private void applyPlan(List<FeeInstallment> plan) {
        var parts = plan == null ? List.<FeeInstallment>of() : plan;
        if (parts.isEmpty()) {
            this.installments = new ArrayList<>();
            touch(Instant.now());
            return;
        }
        if (!allowInstallment) {
            throw new RuleException("Turn on installments before adding an installment plan");
        }
        if (parts.size() < 2) {
            throw new RuleException("An installment plan needs at least two parts");
        }
        var cleaned = new ArrayList<FeeInstallment>();
        BigDecimal total = BigDecimal.ZERO;
        LocalDate previous = null;
        for (int i = 0; i < parts.size(); i++) {
            var part = parts.get(i);
            if (part.amount() == null || part.amount().signum() <= 0) {
                throw new RuleException("Each installment needs an amount above zero");
            }
            if (part.dueDate() == null) {
                throw new RuleException("Each installment needs a due date");
            }
            if (previous != null && part.dueDate().isBefore(previous)) {
                throw new RuleException("Installments have to be in due-date order");
            }
            previous = part.dueDate();
            String label = part.label() == null || part.label().isBlank() ? ordinal(i + 1) + " installment"
                    : part.label().trim();
            var amount = part.amount().setScale(2, RoundingMode.HALF_UP);
            cleaned.add(new FeeInstallment(label, amount, part.dueDate()));
            total = total.add(amount);
        }
        if (total.compareTo(amount.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new RuleException("The installments add up to " + total.toPlainString() + " but the fee is "
                    + amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        }
        this.installments = cleaned;
        this.dueDate = previous;
        touch(Instant.now());
    }

    public boolean hasInstallmentPlan() {
        return installments != null && !installments.isEmpty();
    }

    /**
     * Each part of the plan for one student: their share of it after discounts, how much their payments cover
     * (payments fill the parts in order) and whether it is paid, part paid, due or overdue. Empty without a plan.
     */
    public List<InstallmentStatus> installmentStatuses(BigDecimal paid, BigDecimal discount, LocalDate today) {
        if (!hasInstallmentPlan()) {
            return List.of();
        }
        BigDecimal net = amount.subtract(orZero(discount)).max(BigDecimal.ZERO);
        BigDecimal left = orZero(paid);
        BigDecimal allocated = BigDecimal.ZERO;
        var out = new ArrayList<InstallmentStatus>();
        for (int i = 0; i < installments.size(); i++) {
            var part = installments.get(i);
            // A discount comes off every part in proportion; the last part takes any rounding.
            BigDecimal share = i == installments.size() - 1 ? net.subtract(allocated)
                    : amount.signum() == 0 ? BigDecimal.ZERO
                            : part.amount().multiply(net).divide(amount, 2, RoundingMode.HALF_UP);
            allocated = allocated.add(share);
            BigDecimal covered = left.min(share).max(BigDecimal.ZERO);
            left = left.subtract(covered);
            InstallmentStatus.State state;
            if (covered.compareTo(share) >= 0) {
                state = InstallmentStatus.State.PAID;
            } else if (part.dueDate().isBefore(today)) {
                state = InstallmentStatus.State.OVERDUE;
            } else if (covered.signum() > 0) {
                state = InstallmentStatus.State.PARTIAL;
            } else {
                state = InstallmentStatus.State.DUE;
            }
            out.add(new InstallmentStatus(part.label(), share, covered, part.dueDate(), state));
        }
        return out;
    }

    /**
     * Is a student behind on this fee? With a plan, when a part whose due date has passed isn't fully covered yet;
     * without one, when the fee isn't fully paid and its due date has passed.
     */
    public boolean isOverdue(BigDecimal paid, BigDecimal discount, LocalDate today) {
        BigDecimal net = amount.subtract(orZero(discount));
        if (orZero(paid).compareTo(net) >= 0) {
            return false;
        }
        if (hasInstallmentPlan()) {
            return installmentStatuses(paid, discount, today).stream()
                    .anyMatch(s -> s.state() == InstallmentStatus.State.OVERDUE);
        }
        return dueDate != null && dueDate.isBefore(today);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String ordinal(int n) {
        return switch (n) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> n + "th";
        };
    }
}
