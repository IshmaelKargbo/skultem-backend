package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

// Copy counts and the derived overdue status are the two things the library screens rely on.
class LibraryLoanTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    private final LibraryCategory category = LibraryCategory.create("school-1", "Fiction", "");

    private LibraryBook book(int copies) {
        return LibraryBook.create("school-1", category, "Things Fall Apart", "Chinua Achebe", null, null, "A1",
                copies);
    }

    private Student student() {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn("student-1");
        when(student.getName()).thenReturn("Aminata Kamara");
        when(student.getLastClass()).thenReturn("JSS 2A");
        return student;
    }

    @Test
    void issuingTakesACopyOffTheShelfAndCopiesTheBorrowerDetails() {
        var book = book(2);

        var loan = LibraryLoan.issueToStudent("school-1", book, student(), TODAY, TODAY.plusDays(14));

        assertThat(book.getAvailableCopies()).isEqualTo(1);
        assertThat(loan.getBorrowerName()).isEqualTo("Aminata Kamara");
        assertThat(loan.getBorrowerClass()).isEqualTo("JSS 2A");
        assertThat(loan.getBorrowerId()).isEqualTo("student-1");
    }

    @Test
    void theLastCopyCantBeIssuedTwice() {
        var book = book(1);
        LibraryLoan.issueToStudent("school-1", book, student(), TODAY, TODAY.plusDays(7));

        assertThatThrownBy(() -> LibraryLoan.issueToStudent("school-1", book, student(), TODAY, TODAY.plusDays(7)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(book.getAvailableCopies()).isZero();
    }

    @Test
    void aDueDateOfTodayOrEarlierIsRejectedWithoutTakingACopy() {
        var book = book(1);

        assertThatThrownBy(() -> LibraryLoan.issueToStudent("school-1", book, student(), TODAY, TODAY.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LibraryLoan.issueToStudent("school-1", book, student(), TODAY, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(book.getAvailableCopies()).isEqualTo(1);

        LibraryLoan.issueToStudent("school-1", book, student(), TODAY, TODAY.plusDays(1));
        assertThat(book.getAvailableCopies()).isZero();
    }

    @Test
    void aLoanIsOverdueOnlyAfterItsDueDateAndUntilItIsReturned() {
        var book = book(1);
        var loan = LibraryLoan.issueToStudent("school-1", book, student(), TODAY, TODAY.plusDays(3));

        assertThat(loan.isOverdue(TODAY.plusDays(3))).isFalse();
        assertThat(loan.isOverdue(TODAY.plusDays(4))).isTrue();
        assertThat(loan.daysOverdue(TODAY.plusDays(6))).isEqualTo(3);

        loan.markReturned(book);

        assertThat(loan.isOverdue(TODAY.plusDays(6))).isFalse();
        assertThat(loan.daysOverdue(TODAY.plusDays(6))).isZero();
    }

    @Test
    void returningPutsTheCopyBackOnceOnly() {
        var book = book(1);
        var loan = LibraryLoan.issueToStudent("school-1", book, student(), TODAY, TODAY.plusDays(3));

        loan.markReturned(book);

        assertThat(book.getAvailableCopies()).isEqualTo(1);
        assertThatThrownBy(() -> loan.markReturned(book)).isInstanceOf(IllegalStateException.class);
        assertThat(book.getAvailableCopies()).isEqualTo(1);
    }

    @Test
    void totalCopiesCantDropBelowTheCopiesOnLoan() {
        var book = book(3);
        LibraryLoan.issueToStudent("school-1", book, student(), TODAY, TODAY.plusDays(3));
        LibraryLoan.issueToStudent("school-1", book, student(), TODAY, TODAY.plusDays(3));

        assertThatThrownBy(() -> book.update(category, "T", "A", null, null, null, 1))
                .isInstanceOf(IllegalStateException.class);

        book.update(category, "T", "A", null, null, null, 5);
        assertThat(book.getTotalCopies()).isEqualTo(5);
        assertThat(book.getAvailableCopies()).isEqualTo(3);
    }
}
