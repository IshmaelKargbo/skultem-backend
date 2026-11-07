package com.moriba.skultem.domain.model;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.vo.FeeInstallment;

import java.math.BigDecimal;
import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/**
 * One row per school controlling the platform fee that school's students are charged - see
 * SeedPlatformFeeForAcademicYearUseCase. The id IS the school's id (one setting per school, no
 * separate schoolId column needed) - before this became per-school, a single row shared by every
 * school lived under the fixed id "platform-fee" (see V32__platform_fee_per_school.sql). Set only
 * through a SYSTEM_ADMIN-only endpoint; no school admin/accountant can read or change it via
 * anything reachable from a school's own screens.
 */
@Getter
public class PlatformFeeSetting extends AggregateRoot<String> {

    /** What every newly-created school is charged per student until a SYSTEM_ADMIN changes it. */
    public static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("150.00");

    private BigDecimal amount;

    /**
     * How it is paid: BY_TERM (the default) - three parts whatever the total, one per term in a three-term year
     * (see planFor) - or FULL, all at once. termShares optionally weights the three parts in percent ([50, 30, 20]);
     * empty means equal thirds.
     */
    public enum InstallmentMode {
        FULL,
        BY_TERM
    }

    private InstallmentMode installmentMode = InstallmentMode.BY_TERM;
    private List<Integer> termShares = List.of();

    public PlatformFeeSetting(String id, BigDecimal amount, Instant createdAt, Instant updatedAt) {
        this(id, amount, InstallmentMode.BY_TERM, List.of(), createdAt, updatedAt);
    }

    public PlatformFeeSetting(String id, BigDecimal amount, InstallmentMode installmentMode, List<Integer> termShares,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.amount = amount;
        this.installmentMode = installmentMode != null ? installmentMode : InstallmentMode.BY_TERM;
        this.termShares = termShares == null ? List.of() : List.copyOf(termShares);
        touch(updatedAt);
    }

    public static PlatformFeeSetting create(String schoolId, BigDecimal amount) {
        Instant now = Instant.now();
        return new PlatformFeeSetting(schoolId, amount, now, now);
    }

    public void updateAmount(BigDecimal amount) {
        this.amount = amount;
        touch(Instant.now());
    }

    public boolean isConfigured() {
        return amount != null && amount.signum() > 0;
    }

    /** Null mode keeps the current one; null shares keep the current split, an empty list means equal. */
    public void updatePlan(InstallmentMode mode, List<Integer> shares) {
        if (mode != null) {
            this.installmentMode = mode;
        }
        if (shares != null) {
            if (!shares.isEmpty()) {
                if (shares.stream().anyMatch(s -> s == null || s < 1 || s > 100)) {
                    throw new RuleException("Each term's share has to be between 1% and 100%");
                }
                if (shares.size() != PARTS) {
                    throw new RuleException("Give a share for each of the " + PARTS + " installments");
                }
                if (shares.stream().mapToInt(Integer::intValue).sum() != 100) {
                    throw new RuleException("The term shares have to add up to 100%");
                }
            }
            this.termShares = List.copyOf(shares);
        }
        touch(Instant.now());
    }

    /** How many parts the platform fee is split into unless Skultem sets a school to pay in full. */
    public static final int PARTS = 3;

    /**
     * The installment plan for a platform fee of this amount: three parts, whatever the total. In a year of three
     * terms each part is a term's, due at that term's end; otherwise the three due dates are spread evenly over the
     * academic year (its last part due on its last day). Empty - pay it all at once - when the mode is FULL or there
     * are no dates to plan against. termShares weights the three parts; otherwise they are equal.
     */
    public List<FeeInstallment> planFor(BigDecimal feeAmount, List<Term> terms, AcademicYear academicYear) {
        if (installmentMode == InstallmentMode.FULL || feeAmount == null) {
            return List.of();
        }
        var ordered = terms == null ? List.<Term>of()
                : terms.stream().sorted(Comparator.comparingInt(Term::getTermNumber)).toList();

        var labels = new ArrayList<String>();
        var dates = new ArrayList<LocalDate>();
        if (ordered.size() == PARTS) {
            ordered.forEach(t -> {
                labels.add(t.getName());
                dates.add(t.getEndDate());
            });
        } else {
            LocalDate start = academicYear != null && academicYear.getStartDate() != null ? academicYear.getStartDate()
                    : ordered.isEmpty() ? null : ordered.get(0).getStartDate();
            LocalDate end = academicYear != null && academicYear.getEndDate() != null ? academicYear.getEndDate()
                    : ordered.isEmpty() ? null : ordered.get(ordered.size() - 1).getEndDate();
            if (start == null || end == null || end.isBefore(start)) {
                return List.of();
            }
            long days = java.time.temporal.ChronoUnit.DAYS.between(start, end);
            for (int i = 0; i < PARTS; i++) {
                labels.add(new String[] { "1st", "2nd", "3rd" }[i] + " installment");
                dates.add(i == PARTS - 1 ? end : start.plusDays(days * (i + 1) / PARTS));
            }
        }

        boolean weighted = termShares.size() == PARTS;
        BigDecimal total = feeAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal allocated = BigDecimal.ZERO;
        var plan = new ArrayList<FeeInstallment>();
        for (int i = 0; i < PARTS; i++) {
            BigDecimal part;
            if (i == PARTS - 1) {
                part = total.subtract(allocated);
            } else if (weighted) {
                part = total.multiply(BigDecimal.valueOf(termShares.get(i))).divide(BigDecimal.valueOf(100), 2,
                        RoundingMode.HALF_UP);
            } else {
                part = total.divide(BigDecimal.valueOf(PARTS), 2, RoundingMode.DOWN);
            }
            allocated = allocated.add(part);
            plan.add(new FeeInstallment(labels.get(i), part, dates.get(i)));
        }
        return plan;
    }

    public static String formatShares(List<Integer> shares) {
        return shares == null || shares.isEmpty() ? null
                : String.join(",", shares.stream().map(String::valueOf).toList());
    }

    public static List<Integer> parseShares(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(stored.split(",")).map(String::trim).filter(v -> !v.isEmpty())
                .map(Integer::valueOf).toList();
    }
}
