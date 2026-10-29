package com.moriba.skultem.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.LibraryLoan;
import com.moriba.skultem.domain.model.LibraryLoan.Status;
import com.moriba.skultem.domain.repository.LibraryLoanRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.LibraryLoanJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.LibraryLoanMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LibraryLoanAdapter implements LibraryLoanRepository {
    // Stand-ins for "no limit" on the due-date window; both fit in a Postgres date column.
    private static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);
    private static final LocalDate LATEST = LocalDate.of(9999, 12, 31);

    private final LibraryLoanJpaRepository repo;

    @Override
    public void save(LibraryLoan domain) {
        repo.save(LibraryLoanMapper.toEntity(domain));
    }

    @Override
    public Optional<LibraryLoan> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(LibraryLoanMapper::toDomain);
    }

    @Override
    public Optional<String> findBookId(String loanId, String schoolId) {
        return repo.findBookId(loanId, schoolId);
    }

    @Override
    public boolean existsActiveByBook(String bookId, String schoolId) {
        return repo.existsByBook_IdAndSchoolIdAndStatus(bookId, schoolId, Status.ISSUED);
    }

    @Override
    public void deleteByBook(String bookId, String schoolId) {
        repo.deleteByBook(bookId, schoolId);
    }

    @Override
    public boolean existsActiveByBookAndBorrower(String bookId, String borrowerId, String schoolId) {
        return repo.existsByBookAndBorrower(bookId, borrowerId, schoolId, Status.ISSUED);
    }

    @Override
    public long countActiveByBorrower(String borrowerId, String schoolId) {
        return repo.countByBorrower(borrowerId, schoolId, Status.ISSUED);
    }

    @Override
    public long countOverdueByBorrower(String borrowerId, String schoolId, LocalDate today) {
        return repo.countDueBeforeByBorrower(borrowerId, schoolId, Status.ISSUED, today);
    }

    @Override
    public Page<LibraryLoan> search(String schoolId, String query, Filter filter, LocalDate today,
            Pageable pageable) {
        List<Status> statuses = List.of(Status.ISSUED, Status.RETURNED);
        LocalDate dueFrom = EARLIEST;
        LocalDate dueTo = LATEST;

        switch (filter == null ? Filter.ALL : filter) {
            case ISSUED -> {
                statuses = List.of(Status.ISSUED);
                dueFrom = today;
            }
            case OVERDUE -> {
                statuses = List.of(Status.ISSUED);
                dueTo = today.minusDays(1);
            }
            case RETURNED -> statuses = List.of(Status.RETURNED);
            case ALL -> {
            }
        }

        return repo.search(schoolId, query == null ? "" : query, statuses, dueFrom, dueTo, pageable)
                .map(LibraryLoanMapper::toDomain);
    }

    @Override
    public Page<LibraryLoan> findByBook(String bookId, String schoolId, Pageable pageable) {
        return repo.findAllByBook_IdAndSchoolIdOrderByIssueDateDescCreatedAtDesc(bookId, schoolId, pageable)
                .map(LibraryLoanMapper::toDomain);
    }

    @Override
    public long countOverdue(String schoolId, LocalDate today) {
        return repo.countBySchoolIdAndStatusAndDueDateBefore(schoolId, Status.ISSUED, today);
    }
}
