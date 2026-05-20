package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class MaterialTransaction extends AggregateRoot<String> {
    private final String schoolId;
    private final Material material;
    private final Direction direction;
    private final int qty;
    private final String note;

    public enum Direction {
        IN, OUT
    }

    public MaterialTransaction(String id, String schoolId, Material material, int qty, Direction direction, String note,
            Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.material = material;
        this.qty = qty;
        this.direction = direction;
        this.note = note;
        touch(updatedAt);
    }

    public static MaterialTransaction create(String schoolId, Material material, int qty, Direction direction,
            String note) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new MaterialTransaction(id, schoolId, material, qty, direction, note, now, now);
    }
}
