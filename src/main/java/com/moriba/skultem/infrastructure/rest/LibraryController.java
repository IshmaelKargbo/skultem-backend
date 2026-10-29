package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.LibraryBookDTO;
import com.moriba.skultem.application.dto.LibraryBorrowerStatusDTO;
import com.moriba.skultem.application.dto.LibrarySettingDTO;
import com.moriba.skultem.application.dto.LibraryCategoryDTO;
import com.moriba.skultem.application.dto.LibraryLoanDTO;
import com.moriba.skultem.application.dto.LibrarySummaryDTO;
import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.services.LibraryService;
import com.moriba.skultem.domain.model.LibraryLoan.BorrowerType;
import com.moriba.skultem.domain.repository.LibraryLoanRepository.Filter;
import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.IssueLibraryBookDTO;
import com.moriba.skultem.infrastructure.rest.dto.SaveLibraryBookDTO;
import com.moriba.skultem.infrastructure.rest.dto.SaveLibraryCategoryDTO;
import com.moriba.skultem.infrastructure.rest.dto.SaveLibrarySettingDTO;
import com.moriba.skultem.infrastructure.security.RequiresModule;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiresModule(FeatureModule.LIBRARY)
@RestController
@RequestMapping("/api/v1/library")
@RequiredArgsConstructor
public class LibraryController {

    private static final String MANAGE = "@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PRINCIPAL', 'SUPER_ADMIN', 'PROPRIETOR', 'LIBRARIAN')";
    private static final String VIEW = "@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PRINCIPAL', 'SUPER_ADMIN', 'PROPRIETOR', 'LIBRARIAN', 'TEACHER')";

    private final LibraryService service;

    @GetMapping("/summary")
    @PreAuthorize(VIEW)
    public ApiResponse<LibrarySummaryDTO> summary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return new ApiResponse<>("success", 200, "Library summary fetched successfully", service.summary(school));
    }

    // Settings & borrowing rules

    @GetMapping("/settings")
    @PreAuthorize(VIEW)
    public ApiResponse<LibrarySettingDTO> getSettings(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return new ApiResponse<>("success", 200, "Library settings fetched successfully", service.getSettings(school));
    }

    @PutMapping("/settings")
    @PreAuthorize(MANAGE)
    public ApiResponse<LibrarySettingDTO> updateSettings(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SaveLibrarySettingDTO param) {
        var res = service.updateSettings(school, param.maxStudentLoans(), param.maxTeacherLoans(),
                param.blockWhenOverdue(), param.defaultLoanDays());
        return new ApiResponse<>("success", 200, "Library settings saved successfully", res);
    }

    @GetMapping("/borrowers/{type}/{id}/status")
    @PreAuthorize(MANAGE)
    public ApiResponse<LibraryBorrowerStatusDTO> borrowerStatus(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String type,
            @PathVariable String id) {
        BorrowerType borrowerType;
        try {
            borrowerType = BorrowerType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Borrower type must be STUDENT or TEACHER");
        }
        return new ApiResponse<>("success", 200, "Borrower status fetched successfully",
                service.borrowerStatus(school, borrowerType, id));
    }

    // Categories

    @GetMapping("/categories")
    @PreAuthorize(VIEW)
    public ApiResponse<List<LibraryCategoryDTO>> listCategories(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) String search) {
        return paged(service.listCategories(school, page, size, search), "Library categories fetched successfully");
    }

    @PostMapping("/categories")
    @PreAuthorize(MANAGE)
    public ApiResponse<LibraryCategoryDTO> createCategory(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SaveLibraryCategoryDTO param) {
        var res = service.createCategory(school, param.name(), param.description());
        return new ApiResponse<>("success", 200, "Library category created successfully", res);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize(MANAGE)
    public ApiResponse<LibraryCategoryDTO> updateCategory(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody SaveLibraryCategoryDTO param) {
        var res = service.updateCategory(school, id, param.name(), param.description());
        return new ApiResponse<>("success", 200, "Library category updated successfully", res);
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize(MANAGE)
    public ApiResponse<Object> deleteCategory(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        service.deleteCategory(school, id);
        return new ApiResponse<>("success", 200, "Library category deleted successfully", null);
    }

    // Books

    @GetMapping("/books")
    @PreAuthorize(VIEW)
    public ApiResponse<List<LibraryBookDTO>> listBooks(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "false") boolean outOfStock) {
        return paged(service.listBooks(school, page, size, search, categoryId, outOfStock), "Books fetched successfully");
    }

    @GetMapping("/books/{id}")
    @PreAuthorize(VIEW)
    public ApiResponse<LibraryBookDTO> getBook(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        return new ApiResponse<>("success", 200, "Book fetched successfully", service.getBook(school, id));
    }

    @GetMapping("/books/{id}/loans")
    @PreAuthorize(VIEW)
    public ApiResponse<List<LibraryLoanDTO>> listBookLoans(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page) {
        return paged(service.listBookLoans(school, id, page, size), "Book loans fetched successfully");
    }

    @PostMapping("/books")
    @PreAuthorize(MANAGE)
    public ApiResponse<LibraryBookDTO> createBook(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SaveLibraryBookDTO param) {
        var res = service.createBook(school, param.categoryId(), param.title(), param.author(), param.isbn(),
                param.publisher(), param.shelf(), param.totalCopies());
        return new ApiResponse<>("success", 200, "Book added successfully", res);
    }

    @PutMapping("/books/{id}")
    @PreAuthorize(MANAGE)
    public ApiResponse<LibraryBookDTO> updateBook(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody SaveLibraryBookDTO param) {
        var res = service.updateBook(school, id, param.categoryId(), param.title(), param.author(), param.isbn(),
                param.publisher(), param.shelf(), param.totalCopies());
        return new ApiResponse<>("success", 200, "Book updated successfully", res);
    }

    @DeleteMapping("/books/{id}")
    @PreAuthorize(MANAGE)
    public ApiResponse<Object> deleteBook(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        service.deleteBook(school, id);
        return new ApiResponse<>("success", 200, "Book deleted successfully", null);
    }

    // Loans

    @GetMapping("/loans")
    @PreAuthorize(VIEW)
    public ApiResponse<List<LibraryLoanDTO>> listLoans(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ALL") String status) {
        return paged(service.listLoans(school, page, size, search, filter(status)), "Loans fetched successfully");
    }

    @PostMapping("/loans")
    @PreAuthorize(MANAGE)
    public ApiResponse<LibraryLoanDTO> issueBook(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody IssueLibraryBookDTO param) {
        var res = service.issueBook(school, param.bookId(), BorrowerType.valueOf(param.borrowerType()),
                param.borrowerId(), param.dueDate());
        return new ApiResponse<>("success", 200, "Book issued successfully", res);
    }

    @PostMapping("/loans/{id}/return")
    @PreAuthorize(MANAGE)
    public ApiResponse<LibraryLoanDTO> returnBook(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        return new ApiResponse<>("success", 200, "Book returned successfully", service.returnBook(school, id));
    }

    @PostMapping("/loans/{id}/remind")
    @PreAuthorize(MANAGE)
    public ApiResponse<LibraryLoanDTO> remindBorrower(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        return new ApiResponse<>("success", 200, "Reminder sent successfully", service.remindBorrower(school, id));
    }

    private static Filter filter(String status) {
        try {
            return Filter.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Status must be ALL, ISSUED, OVERDUE or RETURNED");
        }
    }

    private static <T> ApiResponse<List<T>> paged(Page<T> res, String message) {
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());
        return new ApiResponse<>("success", 200, message, res.getContent(), meta);
    }
}
