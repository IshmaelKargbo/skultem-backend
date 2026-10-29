package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import com.moriba.skultem.application.dto.LibraryLoanDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.LibraryMapper;
import com.moriba.skultem.domain.repository.LibraryBookRepository;
import com.moriba.skultem.domain.repository.LibraryLoanRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class ReturnLibraryBookUseCase {
    private final LibraryBookRepository bookRepo;
    private final LibraryLoanRepository loanRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_BOOK_RETURNED")
    public LibraryLoanDTO execute(String schoolId, String loanId) {
        // Lock the book before loading the loan, so the returned copy is added to the latest count
        // and two returns of the same loan at once can't both go through.
        String bookId = loanRepo.findBookId(loanId, schoolId)
                .orElseThrow(() -> new NotFoundException("Loan not found"));
        var book = bookRepo.findByIdAndSchoolForUpdate(bookId, schoolId)
                .orElseThrow(() -> new NotFoundException("Book not found"));

        var loan = loanRepo.findByIdAndSchool(loanId, schoolId)
                .orElseThrow(() -> new NotFoundException("Loan not found"));

        if (loan.isReturned()) {
            throw new RuleException("This book has already been returned");
        }

        loan.markReturned(book);

        bookRepo.save(book);
        loanRepo.save(loan);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library book returned",
                book.getTitle() + " - " + loan.getBorrowerName(), null, loan.getId());

        return LibraryMapper.toDTO(loan, LocalDate.now());
    }
}
