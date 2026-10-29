package com.moriba.skultem.application.dto;

import java.time.Instant;

public record LibraryBookDTO(String id, String title, String author, String isbn, String publisher, String shelf,
        LibraryCategoryDTO category, int totalCopies, int availableCopies, Instant addedAt, Instant updatedAt) {
}
