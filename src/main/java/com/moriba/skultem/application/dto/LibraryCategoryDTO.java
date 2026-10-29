package com.moriba.skultem.application.dto;

import java.time.Instant;

public record LibraryCategoryDTO(String id, String name, String description, long bookCount, Instant createdAt,
        Instant updatedAt) {
}
