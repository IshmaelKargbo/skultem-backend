package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Material extends AggregateRoot<String> {
    private String schoolId;
    private String name;
    private Unit unit;
    private MaterialCategory category;
    private Double stockQuantity;
    private Double reorderLevel;
    private Instant lastRestockedAt;

    public enum Unit {
        PCS, BOX, PACK, LITRE
    }

    public Material(String id, String schoolId, String name, Unit unit, MaterialCategory category, Double stockQuantity,
            Double reorderLevel, Instant lastRestockedAt, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.category = category;
        this.stockQuantity = stockQuantity;
        this.reorderLevel = reorderLevel;
        this.lastRestockedAt = lastRestockedAt;
        touch(updatedAt);
    }

    public static Material create(String schoolId, String name, Unit unit, MaterialCategory category) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new Material(id, schoolId, name, unit, category, 0.0, 0.0, null, now, now);
    }
}
