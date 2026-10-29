package com.moriba.skultem.application.mapper;

import java.time.LocalDate;

import com.moriba.skultem.application.dto.LibraryBookDTO;
import com.moriba.skultem.application.dto.LibraryCategoryDTO;
import com.moriba.skultem.application.dto.LibraryLoanDTO;
import com.moriba.skultem.domain.model.LibraryBook;
import com.moriba.skultem.domain.model.LibraryCategory;
import com.moriba.skultem.domain.model.LibraryLoan;

public class LibraryMapper {

    public static LibraryCategoryDTO toDTO(LibraryCategory param, long bookCount) {
        if (param == null) return null;
        return new LibraryCategoryDTO(param.getId(), param.getName(), param.getDescription(), bookCount,
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static LibraryCategoryDTO toDTO(LibraryCategory param) {
        return toDTO(param, 0);
    }

    public static LibraryBookDTO toDTO(LibraryBook param) {
        if (param == null) return null;
        return new LibraryBookDTO(param.getId(), param.getTitle(), param.getAuthor(), param.getIsbn(),
                param.getPublisher(), param.getShelf(), toDTO(param.getCategory()), param.getTotalCopies(),
                param.getAvailableCopies(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static LibraryLoanDTO toDTO(LibraryLoan param, LocalDate today) {
        if (param == null) return null;

        String status = param.isReturned() ? "RETURNED" : param.isOverdue(today) ? "OVERDUE" : "ISSUED";

        return new LibraryLoanDTO(param.getId(), toDTO(param.getBook()), param.getBorrowerType().name(),
                param.getBorrowerId(), param.getBorrowerName(), param.getBorrowerClass(), param.getIssueDate(),
                param.getDueDate(), param.getReturnedAt(), status, param.daysOverdue(today), param.getNotifiedAt());
    }
}
