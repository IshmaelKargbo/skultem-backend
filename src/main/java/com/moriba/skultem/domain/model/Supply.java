package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Supply extends AggregateRoot<String> {
    private String schoolId;
    private Student student;
    private Material material;
    private int qty;
    private int collectedQty;
    private Status status;
    private Instant collectedOn;

    public enum Status {
        PENDING,
        PARTIAL,
        COLLECTED,
        CANCELLED
    }

    public Supply(String id, String schoolId, Student student, Material material, int qty, int collectedQty,
            Status status, Instant collectedOn, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.student = student;
        this.material = material;
        this.qty = qty;
        this.collectedQty = collectedQty;
        this.status = status;
        this.collectedOn = collectedOn;
        touch(updatedAt);
    }

    public static Supply create(String schoolId, Student student, Material material, int qty) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new Supply(id, schoolId, student, material, qty, 0, Status.PENDING, null, now, now);
    }

    public void collect(int qtyToCollect) {

        if (status == Status.CANCELLED) {
            throw new IllegalStateException("Cannot collect cancelled supply request");
        }

        if (status == Status.COLLECTED) {
            throw new IllegalStateException("Supply already fully collected");
        }

        int remaining = this.qty - this.collectedQty;

        if (qtyToCollect <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        if (qtyToCollect > remaining) {
            throw new IllegalArgumentException("Cannot collect more than remaining quantity");
        }

        this.collectedQty += qtyToCollect;

        if (this.collectedQty == 0) {
            this.status = Status.PENDING;
        } else if (this.collectedQty < this.qty) {
            this.status = Status.PARTIAL;
        } else {
            this.status = Status.COLLECTED;
            this.collectedOn = Instant.now();
        }

        touch(Instant.now());
    }
}
