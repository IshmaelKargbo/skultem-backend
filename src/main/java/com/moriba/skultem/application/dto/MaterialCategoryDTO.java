package com.moriba.skultem.application.dto;

import java.time.Instant;

public record MaterialCategoryDTO(String id, String name, String description, Instant createdAt,
                Instant updatedAt) {
}
