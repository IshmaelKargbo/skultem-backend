package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.Getter;

/**
 * The single place stock actually gets deducted for handing a material to someone - whether they
 * were entitled to it via a fully-paid fee ({@link #sourceSaleId} null, created by
 * {@code CreateSupplyUseCase} via {@code RecordPaymentUseCase#processSupply}), or they bought it
 * outright via {@link MaterialSale} ({@link #sourceSaleId} set, created alongside the sale by
 * {@code CreateMaterialSaleUseCase}). A sale never deducts stock itself - it always goes through a
 * linked Supply record, collected immediately if stock's on hand or later once it arrives, exactly
 * like a fee-entitled Supply already worked. See {@code SupplyMaterialUseCase}, the one place that
 * collection actually happens.
 */
@Getter
public class Supply extends AggregateRoot<String> {
    private String schoolId;
    private Student student;
    // Walk-in buyer, set only when student is null - mirrors MaterialSale's own buyer fields,
    // since a sale to a walk-in customer still needs somewhere to record who's collecting it.
    private String customerName;
    private Material material;
    private int qty;
    private int collectedQty;
    private Status status;
    private Instant collectedOn;
    // Set only when this Supply was created by a sale rather than a paid fee - lets
    // GetPendingPickupsUseCase look up that sale's payment status, and lets a cancelled sale cancel
    // its own linked Supply without touching fee-entitled ones.
    private String sourceSaleId;

    public enum Status {
        PENDING,
        PARTIAL,
        COLLECTED,
        CANCELLED
    }

    public Supply(String id, String schoolId, Student student, String customerName, Material material, int qty,
            int collectedQty, Status status, Instant collectedOn, String sourceSaleId, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.student = student;
        this.customerName = customerName;
        this.material = material;
        this.qty = qty;
        this.collectedQty = collectedQty;
        this.status = status;
        this.collectedOn = collectedOn;
        this.sourceSaleId = sourceSaleId;
        touch(updatedAt);
    }

    // Fee-entitled supply - always student-linked, never tied to a sale.
    public static Supply create(String schoolId, Student student, Material material, int qty) {
        return create(schoolId, student, null, material, qty, null);
    }

    // Sale-linked supply - student XOR customerName, tagged with the sale it came from.
    public static Supply create(String schoolId, Student student, String customerName, Material material, int qty,
            String sourceSaleId) {
        if (student == null && (customerName == null || customerName.isBlank())) {
            throw new IllegalArgumentException("A supply must be linked to a student or have a customer name");
        }

        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new Supply(id, schoolId, student, student == null ? customerName.trim() : null, material, qty, 0,
                Status.PENDING, null, sourceSaleId, now, now);
    }

    public String getBuyerName() {
        return student != null ? student.getName() : customerName;
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

    // Only while nothing's been collected yet - once any quantity is out the door there's stock to
    // account for, which is a return/refund, not a plain cancel.
    public void cancel() {
        if (status == Status.CANCELLED) {
            throw new IllegalStateException("Supply is already cancelled");
        }

        if (collectedQty > 0) {
            throw new IllegalStateException("Cannot cancel a supply that's already been partially collected");
        }

        this.status = Status.CANCELLED;
        touch(Instant.now());
    }
}
