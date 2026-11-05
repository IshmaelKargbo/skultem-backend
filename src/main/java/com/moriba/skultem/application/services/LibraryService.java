package com.moriba.skultem.application.services;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LibraryBookDTO;
import com.moriba.skultem.application.dto.LibraryBorrowerStatusDTO;
import com.moriba.skultem.application.dto.LibrarySettingDTO;
import com.moriba.skultem.application.dto.LibraryCategoryDTO;
import com.moriba.skultem.application.dto.LibraryLoanDTO;
import com.moriba.skultem.application.dto.LibrarySummaryDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.LibraryMapper;
import com.moriba.skultem.application.usecase.CreateLibraryBookUseCase;
import com.moriba.skultem.application.usecase.CreateLibraryCategoryUseCase;
import com.moriba.skultem.application.usecase.DeleteLibraryBookUseCase;
import com.moriba.skultem.application.usecase.DeleteLibraryCategoryUseCase;
import com.moriba.skultem.application.usecase.IssueLibraryBookUseCase;
import com.moriba.skultem.application.usecase.RemindLibraryBorrowerUseCase;
import com.moriba.skultem.application.usecase.ReturnLibraryBookUseCase;
import com.moriba.skultem.application.usecase.UpdateLibraryBookUseCase;
import com.moriba.skultem.application.usecase.UpdateLibraryCategoryUseCase;
import com.moriba.skultem.application.usecase.UpdateLibrarySettingUseCase;
import com.moriba.skultem.domain.model.LibraryCategory;
import com.moriba.skultem.domain.model.LibrarySetting;
import com.moriba.skultem.domain.model.LibraryLoan.BorrowerType;
import com.moriba.skultem.domain.repository.LibraryBookRepository;
import com.moriba.skultem.domain.repository.LibraryCategoryRepository;
import com.moriba.skultem.domain.repository.LibraryLoanRepository;
import com.moriba.skultem.domain.repository.LibraryLoanRepository.Filter;
import com.moriba.skultem.domain.repository.LibrarySettingRepository;

import java.util.List;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.LibraryBook;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final LibraryCategoryRepository categoryRepo;
    private final LibraryBookRepository bookRepo;
    private final LibraryLoanRepository loanRepo;
    private final LibrarySettingRepository settingRepo;
    private final UpdateLibrarySettingUseCase updateSettingUseCase;

    private final CreateLibraryCategoryUseCase createCategoryUseCase;
    private final UpdateLibraryCategoryUseCase updateCategoryUseCase;
    private final DeleteLibraryCategoryUseCase deleteCategoryUseCase;
    private final CreateLibraryBookUseCase createBookUseCase;
    private final UpdateLibraryBookUseCase updateBookUseCase;
    private final DeleteLibraryBookUseCase deleteBookUseCase;
    private final IssueLibraryBookUseCase issueBookUseCase;
    private final ReturnLibraryBookUseCase returnBookUseCase;
    private final RemindLibraryBorrowerUseCase remindBorrowerUseCase;
    // Who sees which section's books - see LibrarySectionAccess.
    private final LibrarySectionAccess access;

    // Categories

    public LibraryCategoryDTO createCategory(String schoolId, String name, String description) {
        return createCategoryUseCase.execute(schoolId, name, description);
    }

    public LibraryCategoryDTO updateCategory(String schoolId, String id, String name, String description) {
        return updateCategoryUseCase.execute(schoolId, id, name, description);
    }

    public void deleteCategory(String schoolId, String id) {
        deleteCategoryUseCase.execute(schoolId, id);
    }

    public Page<LibraryCategoryDTO> listCategories(String schoolId, int page, int size, String search) {
        Page<LibraryCategory> result = categoryRepo.search(schoolId, trim(search), pageable(page, size));
        var counts = bookRepo.countByCategories(schoolId, result.map(LibraryCategory::getId).getContent(),
                access.visibleSections());
        return result.map(c -> LibraryMapper.toDTO(c, counts.getOrDefault(c.getId(), 0L)));
    }

    // Books

    public LibraryBookDTO createBook(String schoolId, String sectionId, String categoryId, String title,
            String author, String isbn, String publisher, String shelf, int totalCopies) {
        return createBookUseCase.execute(schoolId, access.sectionForSave(schoolId, sectionId), categoryId, title,
                author, isbn, publisher, shelf, totalCopies);
    }

    public LibraryBookDTO updateBook(String schoolId, String id, String sectionId, String categoryId, String title,
            String author, String isbn, String publisher, String shelf, int totalCopies) {
        visibleBook(schoolId, id);
        return updateBookUseCase.execute(schoolId, id, access.sectionForSave(schoolId, sectionId), categoryId,
                title, author, isbn, publisher, shelf, totalCopies);
    }

    public void deleteBook(String schoolId, String id) {
        visibleBook(schoolId, id);
        deleteBookUseCase.execute(schoolId, id);
    }

    public LibraryBookDTO getBook(String schoolId, String id) {
        return LibraryMapper.toDTO(visibleBook(schoolId, id));
    }

    // sectionId narrows the list to one section; it can't widen it past the caller's own sections.
    public Page<LibraryBookDTO> listBooks(String schoolId, int page, int size, String search, String categoryId,
            boolean outOfStockOnly, String sectionId) {
        return bookRepo.search(schoolId, trim(search), trim(categoryId), outOfStockOnly, sections(sectionId),
                pageable(page, size))
                .map(LibraryMapper::toDTO);
    }

    // Loans

    public LibraryLoanDTO issueBook(String schoolId, String bookId, BorrowerType borrowerType, String borrowerId,
            LocalDate dueDate) {
        visibleBook(schoolId, bookId);
        return issueBookUseCase.execute(schoolId, bookId, borrowerType, borrowerId, dueDate);
    }

    public LibraryLoanDTO returnBook(String schoolId, String loanId) {
        visibleLoan(schoolId, loanId);
        return returnBookUseCase.execute(schoolId, loanId);
    }

    public LibraryLoanDTO remindBorrower(String schoolId, String loanId) {
        visibleLoan(schoolId, loanId);
        return remindBorrowerUseCase.execute(schoolId, loanId);
    }

    public Page<LibraryLoanDTO> listLoans(String schoolId, int page, int size, String search, Filter filter,
            String sectionId) {
        LocalDate today = LocalDate.now();
        return loanRepo.search(schoolId, trim(search), filter, today, sections(sectionId), pageable(page, size))
                .map(loan -> LibraryMapper.toDTO(loan, today));
    }

    public Page<LibraryLoanDTO> listBookLoans(String schoolId, String bookId, int page, int size) {
        visibleBook(schoolId, bookId);
        LocalDate today = LocalDate.now();
        return loanRepo.findByBook(bookId, schoolId, pageable(page, size))
                .map(loan -> LibraryMapper.toDTO(loan, today));
    }

    public LibrarySummaryDTO summary(String schoolId, String sectionId) {
        var visible = sections(sectionId);
        var totals = bookRepo.totals(schoolId, visible);
        return new LibrarySummaryDTO(
                totals.titles(),
                totals.totalCopies(),
                totals.availableCopies(),
                totals.totalCopies() - totals.availableCopies(),
                loanRepo.countOverdue(schoolId, LocalDate.now(), visible),
                categoryRepo.countBySchool(schoolId));
    }

    // Settings & borrowing rules

    public LibrarySettingDTO getSettings(String schoolId) {
        return UpdateLibrarySettingUseCase.toDTO(setting(schoolId));
    }

    public LibrarySettingDTO updateSettings(String schoolId, int maxStudentLoans, int maxTeacherLoans,
            boolean blockWhenOverdue, int defaultLoanDays) {
        return updateSettingUseCase.execute(schoolId, maxStudentLoans, maxTeacherLoans, blockWhenOverdue,
                defaultLoanDays);
    }

    /** What the issue form shows once a borrower is picked - the same check issuing a book runs. */
    public LibraryBorrowerStatusDTO borrowerStatus(String schoolId, BorrowerType type, String borrowerId) {
        var setting = setting(schoolId);
        long active = loanRepo.countActiveByBorrower(borrowerId, schoolId);
        long overdue = loanRepo.countOverdueByBorrower(borrowerId, schoolId, LocalDate.now());
        String reason = setting.blockReason(type, active, overdue);
        return new LibraryBorrowerStatusDTO(active, overdue, setting.maxLoansFor(type), reason == null, reason);
    }

    // The sections a list covers: the caller's own, optionally narrowed to one of them.
    private List<String> sections(String sectionId) {
        var visible = access.visibleSections();
        String wanted = trim(sectionId);
        if (wanted.isEmpty()) {
            return visible;
        }
        if (visible != null && !visible.contains(wanted)) {
            throw new RuleException("You can only see your own sections' books");
        }
        return List.of(wanted);
    }

    private LibraryBook visibleBook(String schoolId, String bookId) {
        var book = bookRepo.findByIdAndSchool(bookId, schoolId)
                .orElseThrow(() -> new NotFoundException("Book not found"));
        access.requireVisible(book);
        return book;
    }

    private void visibleLoan(String schoolId, String loanId) {
        String bookId = loanRepo.findBookId(loanId, schoolId)
                .orElseThrow(() -> new NotFoundException("Loan not found"));
        visibleBook(schoolId, bookId);
    }

    private LibrarySetting setting(String schoolId) {
        return settingRepo.findBySchool(schoolId).orElseGet(() -> LibrarySetting.defaults(schoolId));
    }

    private static Pageable pageable(int page, int size) {
        return size > 0 ? PageRequest.of(Math.max(page, 1) - 1, size) : Pageable.unpaged();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
