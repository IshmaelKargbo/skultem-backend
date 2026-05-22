package com.moriba.skultem.application.dto;

import java.math.BigInteger;
import java.time.Instant;

import com.moriba.skultem.domain.model.Material.Unit;

public record MaterialDTO(String id, String name, Unit unit, MaterialCategoryDTO category, BigInteger inStock,
        int reorderLevel, Instant lastRestockedAt, Instant createdAt, Instant updatedAt) {
}
