package com.moriba.skultem.application.dto;

import java.time.Instant;

public record ExpenseCategoryDTO(String id, String name, String description, Instant createdAt,
                Instant updatedAt) {
}
