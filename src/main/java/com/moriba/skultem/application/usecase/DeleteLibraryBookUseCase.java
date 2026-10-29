package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.repository.LibraryBookRepository;
import com.moriba.skultem.domain.repository.LibraryLoanRepository;

/**
 * Removes a book from the catalog together with its past (returned) loans. Blocked while any copy
 * is still out on loan - those have to come back first.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteLibraryBookUseCase {
    private final LibraryBookRepository repo;
    private final LibraryLoanRepository loanRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_BOOK_DELETED")
    public void execute(String schoolId, String id) {
        var book = repo.findByIdAndSchoolForUpdate(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Book not found"));

        if (loanRepo.existsActiveByBook(id, schoolId)) {
            throw new RuleException("Copies of this book are still on loan. Mark them as returned before deleting it.");
        }

        loanRepo.deleteByBook(id, schoolId);
        repo.deleteById(id);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library book deleted", book.getTitle(), null, id);
    }
}
