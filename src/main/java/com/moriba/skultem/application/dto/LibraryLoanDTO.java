package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * {@code status} is ISSUED, OVERDUE or RETURNED. OVERDUE isn't stored: it's an issued loan whose
 * due date has passed, worked out when the loan is read.
 */
public record LibraryLoanDTO(String id, LibraryBookDTO book, String borrowerType, String borrowerId,
        String borrowerName, String borrowerClass, LocalDate issueDate, LocalDate dueDate, Instant returnDate,
        String status, long daysOverdue, Instant notifiedAt) {
}
