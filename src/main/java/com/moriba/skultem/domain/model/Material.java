package com.moriba.skultem.domain.model;

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
    private int stockQuantity;
    private int reorderLevel;
    private Instant lastRestockedAt;

    public enum Unit {
        PCS, BOX, PACK, LITRE
    }

    public Material(String id, String schoolId, String name, Unit unit, MaterialCategory category, int stockQuantity,
            int reorderLevel, Instant lastRestockedAt, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.unit = unit;
        this.category = category;
        this.stockQuantity = stockQuantity;
        this.reorderLevel = reorderLevel;
        this.lastRestockedAt = lastRestockedAt;
        touch(updatedAt);
    }

    public static Material create(String schoolId, String name, Unit unit, int qty, MaterialCategory category) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new Material(id, schoolId, name, unit, category, qty, 0, null, now, now);
    }

    public void stock(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        this.stockQuantity += qty;
        this.lastRestockedAt = Instant.now();

        touch(Instant.now());
    }

    public void supply(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        this.stockQuantity -= qty;
        this.lastRestockedAt = Instant.now();

        touch(Instant.now());
    }
}
