package com.moriba.skultem.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Single place a school-fee collection rate is computed, and the one canonical status a student's
 * school-fee balance resolves to for the Fees Reporting feature (dashboard / term summary /
 * student balances / outstanding). Mirrors AttendanceRateCalculator's role for attendance - every
 * fee report use case calls this instead of re-deriving the math, so the numbers can never drift
 * between them.
 *
 * This is deliberately a *different*, simpler status vocabulary than StudentFeeMapper's
 * (Paid/Overdue/Partial/Pending) or FinanceReportUseCase's (Paid/Unpaid/Overdue/Partial) - those
 * back the existing per-fee and payment-eligibility screens and are left untouched. "Outstanding"
 * here is a report-level term (any row with a balance > 0), not a fourth per-row status: a row's
 * status is exactly one of PAID / PARTIALLY_PAID / NO_PAYMENT.
 */
public final class FeeCollectionCalculator {

    private FeeCollectionCalculator() {
    }

    public enum Status {
        PAID,
        PARTIALLY_PAID,
        NO_PAYMENT
    }

    public static Status resolveStatus(BigDecimal netPayable, BigDecimal paid) {
        if (netPayable.compareTo(BigDecimal.ZERO) <= 0 || paid.compareTo(netPayable) >= 0) {
            return Status.PAID;
        }
        if (paid.compareTo(BigDecimal.ZERO) > 0) {
            return Status.PARTIALLY_PAID;
        }
        return Status.NO_PAYMENT;
    }

    public static BigDecimal balance(BigDecimal netPayable, BigDecimal paid) {
        BigDecimal balance = netPayable.subtract(paid);
        return balance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : balance;
    }

    /** Collection rate as a 0-100 percentage, rounded to one decimal place; 0 when nothing was expected. */
    public static double rate(BigDecimal collected, BigDecimal expected) {
        if (expected == null || expected.compareTo(BigDecimal.ZERO) <= 0 || collected == null) {
            return 0.0;
        }
        return collected.divide(expected, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
