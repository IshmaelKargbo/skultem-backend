package com.moriba.skultem.application.dto;

import java.time.Instant;

public record FeeCategoryDTO(String id, String name, String description, boolean system, Instant createdAt,
                Instant updatedAt) {
}
