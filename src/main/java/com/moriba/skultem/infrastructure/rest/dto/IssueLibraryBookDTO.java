package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record IssueLibraryBookDTO(
        @NotBlank(message = "Book is required") String bookId,
        @NotBlank(message = "Borrower type is required") @Pattern(regexp = "STUDENT|TEACHER", message = "Borrower type must be STUDENT or TEACHER") String borrowerType,
        @NotBlank(message = "Borrower is required") String borrowerId,
        @NotNull(message = "Due date is required") LocalDate dueDate) {
}
