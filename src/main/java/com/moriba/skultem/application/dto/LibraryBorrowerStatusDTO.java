package com.moriba.skultem.application.dto;

/**
 * Where a borrower stands before a new loan: what they hold, their limit, and - when they can't
 * borrow - why not ({@code reason} is null when {@code canBorrow} is true).
 */
public record LibraryBorrowerStatusDTO(long activeLoans, long overdueLoans, int maxLoans, boolean canBorrow,
        String reason) {
}
