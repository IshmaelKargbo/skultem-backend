package com.moriba.skultem.domain.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.LibraryLoan;

public interface LibraryLoanRepository {

    /** The status filter on the loan list. OVERDUE and ISSUED are both stored as ISSUED; the due date tells them apart. */
    enum Filter {
        ALL, ISSUED, OVERDUE, RETURNED
    }

    void save(LibraryLoan domain);

    Optional<LibraryLoan> findByIdAndSchool(String id, String schoolId);

    // Just the book id, so a caller can lock the book before loading the loan with it.
    Optional<String> findBookId(String loanId, String schoolId);

    boolean existsActiveByBook(String bookId, String schoolId);

    // Removes every loan of a book; only called once none of them is still active.
    void deleteByBook(String bookId, String schoolId);

    boolean existsActiveByBookAndBorrower(String bookId, String borrowerId, String schoolId);

    // Books a borrower holds right now (overdue ones included).
    long countActiveByBorrower(String borrowerId, String schoolId);

    // How many of the books a borrower holds are past their due date.
    long countOverdueByBorrower(String borrowerId, String schoolId, LocalDate today);

    // Matches on book title or borrower name; an empty query means "no search".
    Page<LibraryLoan> search(String schoolId, String query, Filter filter, LocalDate today, Pageable pageable);

    Page<LibraryLoan> findByBook(String bookId, String schoolId, Pageable pageable);

    long countOverdue(String schoolId, LocalDate today);
}
