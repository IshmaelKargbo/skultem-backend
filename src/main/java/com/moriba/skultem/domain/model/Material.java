package com.moriba.skultem.domain.model;

import java.math.BigInteger;
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
    private BigInteger stockQuantity;
    private int reorderLevel;
    private Instant lastRestockedAt;

    public enum Unit {
        PCS, BOX, PACK, LITRE
    }

    public Material(
            String id,
            String schoolId,
            String name,
            Unit unit,
            MaterialCategory category,
            BigInteger stockQuantity,
            int reorderLevel,
            Instant lastRestockedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.unit = unit;
        this.category = category;
        this.stockQuantity = stockQuantity != null ? stockQuantity : BigInteger.ZERO;
        this.reorderLevel = reorderLevel;
        this.lastRestockedAt = lastRestockedAt;

        touch(updatedAt);
    }

    // FACTORY METHOD
    public static Material create(
            String schoolId,
            String name,
            Unit unit,
            BigInteger qty,
            MaterialCategory category
    ) {
        Instant now = Instant.now();

        if (qty == null || qty.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Initial quantity cannot be negative");
        }

        return new Material(
                UUID.randomUUID().toString(),
                schoolId,
                name,
                unit,
                category,
                qty,
                0,
                null,
                now,
                now
        );
    }

    // ADD STOCK
    public void stock(int qty) {
        validateQty(qty);

        this.stockQuantity = this.stockQuantity.add(BigInteger.valueOf(qty));
        this.lastRestockedAt = Instant.now();

        touch(Instant.now());
    }

    // DEDUCT STOCK
    public void deduct(int qty) {
        validateQty(qty);

        BigInteger deductQty = BigInteger.valueOf(qty);

        if (this.stockQuantity.compareTo(deductQty) < 0) {
            throw new IllegalStateException(
                    "Insufficient stock. Available: " + stockQuantity + ", requested: " + qty
            );
        }

        this.stockQuantity = this.stockQuantity.subtract(deductQty);

        touch(Instant.now());
    }

    private void validateQty(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }
}