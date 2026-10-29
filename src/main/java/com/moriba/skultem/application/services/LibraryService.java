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
        var counts = bookRepo.countByCategories(schoolId, result.map(LibraryCategory::getId).getContent());
        return result.map(c -> LibraryMapper.toDTO(c, counts.getOrDefault(c.getId(), 0L)));
    }

    // Books

    public LibraryBookDTO createBook(String schoolId, String categoryId, String title, String author, String isbn,
            String publisher, String shelf, int totalCopies) {
        return createBookUseCase.execute(schoolId, categoryId, title, author, isbn, publisher, shelf, totalCopies);
    }

    public LibraryBookDTO updateBook(String schoolId, String id, String categoryId, String title, String author,
            String isbn, String publisher, String shelf, int totalCopies) {
        return updateBookUseCase.execute(schoolId, id, categoryId, title, author, isbn, publisher, shelf,
                totalCopies);
    }

    public void deleteBook(String schoolId, String id) {
        deleteBookUseCase.execute(schoolId, id);
    }

    public LibraryBookDTO getBook(String schoolId, String id) {
        return bookRepo.findByIdAndSchool(id, schoolId)
                .map(LibraryMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Book not found"));
    }

    public Page<LibraryBookDTO> listBooks(String schoolId, int page, int size, String search, String categoryId,
            boolean outOfStockOnly) {
        return bookRepo.search(schoolId, trim(search), trim(categoryId), outOfStockOnly, pageable(page, size))
                .map(LibraryMapper::toDTO);
    }

    // Loans

    public LibraryLoanDTO issueBook(String schoolId, String bookId, BorrowerType borrowerType, String borrowerId,
            LocalDate dueDate) {
        return issueBookUseCase.execute(schoolId, bookId, borrowerType, borrowerId, dueDate);
    }

    public LibraryLoanDTO returnBook(String schoolId, String loanId) {
        return returnBookUseCase.execute(schoolId, loanId);
    }

    public LibraryLoanDTO remindBorrower(String schoolId, String loanId) {
        return remindBorrowerUseCase.execute(schoolId, loanId);
    }

    public Page<LibraryLoanDTO> listLoans(String schoolId, int page, int size, String search, Filter filter) {
        LocalDate today = LocalDate.now();
        return loanRepo.search(schoolId, trim(search), filter, today, pageable(page, size))
                .map(loan -> LibraryMapper.toDTO(loan, today));
    }

    public Page<LibraryLoanDTO> listBookLoans(String schoolId, String bookId, int page, int size) {
        LocalDate today = LocalDate.now();
        return loanRepo.findByBook(bookId, schoolId, pageable(page, size))
                .map(loan -> LibraryMapper.toDTO(loan, today));
    }

    public LibrarySummaryDTO summary(String schoolId) {
        var totals = bookRepo.totals(schoolId);
        return new LibrarySummaryDTO(
                totals.titles(),
                totals.totalCopies(),
                totals.availableCopies(),
                totals.totalCopies() - totals.availableCopies(),
                loanRepo.countOverdue(schoolId, LocalDate.now()),
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
