package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.model.LibraryLoan.BorrowerType;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/**
 * A school's borrowing rules: how many books a student or teacher may hold at once, whether an
 * overdue book stops them borrowing more, and how long a book is lent for by default.
 */
@Getter
public class LibrarySetting extends AggregateRoot<String> {
    public static final int MAX_LIMIT = 50;
    public static final int MAX_LOAN_DAYS = 365;

    private String schoolId;
    private int maxStudentLoans;
    private int maxTeacherLoans;
    private boolean blockWhenOverdue;
    private int defaultLoanDays;

    public LibrarySetting(String id, String schoolId, int maxStudentLoans, int maxTeacherLoans,
            boolean blockWhenOverdue, int defaultLoanDays, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.maxStudentLoans = maxStudentLoans;
        this.maxTeacherLoans = maxTeacherLoans;
        this.blockWhenOverdue = blockWhenOverdue;
        this.defaultLoanDays = defaultLoanDays;
        touch(updatedAt);
    }

    /** The rules a school uses until it saves its own. */
    public static LibrarySetting defaults(String schoolId) {
        Instant now = Instant.now();
        return new LibrarySetting(UUID.randomUUID().toString(), schoolId, 2, 5, true, 14, now, now);
    }

    public void update(int maxStudentLoans, int maxTeacherLoans, boolean blockWhenOverdue, int defaultLoanDays) {
        if (maxStudentLoans < 1 || maxStudentLoans > MAX_LIMIT || maxTeacherLoans < 1 || maxTeacherLoans > MAX_LIMIT) {
            throw new IllegalArgumentException("Borrowing limits must be between 1 and " + MAX_LIMIT + " books");
        }
        if (defaultLoanDays < 1 || defaultLoanDays > MAX_LOAN_DAYS) {
            throw new IllegalArgumentException("Loan period must be between 1 and " + MAX_LOAN_DAYS + " days");
        }

        this.maxStudentLoans = maxStudentLoans;
        this.maxTeacherLoans = maxTeacherLoans;
        this.blockWhenOverdue = blockWhenOverdue;
        this.defaultLoanDays = defaultLoanDays;
        touch(Instant.now());
    }

    public int maxLoansFor(BorrowerType type) {
        return type == BorrowerType.TEACHER ? maxTeacherLoans : maxStudentLoans;
    }

    /**
     * Why this borrower can't take another book right now, or null if they can.
     *
     * @param activeLoans  books they hold now (overdue ones included)
     * @param overdueLoans how many of those are past their due date
     */
    public String blockReason(BorrowerType type, long activeLoans, long overdueLoans) {
        if (blockWhenOverdue && overdueLoans > 0) {
            return overdueLoans == 1
                    ? "Has an overdue book. It must be returned before borrowing another."
                    : "Has " + overdueLoans + " overdue books. They must be returned before borrowing another.";
        }

        int max = maxLoansFor(type);
        if (activeLoans >= max) {
            return "Already has " + activeLoans + " of " + max + " books allowed. Return one before borrowing another.";
        }

        return null;
    }
}
