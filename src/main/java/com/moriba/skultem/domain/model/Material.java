package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
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
    // The selling price per unit - lets a sale pre-fill its unit price from the catalog instead
    // of someone re-typing (and potentially mis-typing) it on every single sale. A sale still
    // captures its own unitPrice snapshot at the time it's made (see MaterialSale), so changing
    // this later never rewrites the price of a sale already recorded.
    private BigDecimal price;
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
            BigDecimal price,
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
        this.price = price != null ? price : BigDecimal.ZERO;
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
            BigDecimal price,
            MaterialCategory category
    ) {
        Instant now = Instant.now();

        if (qty == null || qty.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Initial quantity cannot be negative");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        return new Material(
                UUID.randomUUID().toString(),
                schoolId,
                name,
                unit,
                category,
                qty,
                price,
                0,
                null,
                now,
                now
        );
    }

    // RENAME / RECATEGORIZE / REPRICE - stock is managed separately via stock()/deduct(), never here
    public void update(String name, Unit unit, MaterialCategory category, BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        this.name = name;
        this.unit = unit;
        this.category = category;
        this.price = price;
        touch(Instant.now());
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