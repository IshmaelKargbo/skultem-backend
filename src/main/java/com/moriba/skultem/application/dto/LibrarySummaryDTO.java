package com.moriba.skultem.application.dto;

public record LibrarySummaryDTO(long titles, long totalCopies, long availableCopies, long issuedCopies,
        long overdueLoans, long categories) {
}
