package com.moriba.skultem.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.moriba.skultem.domain.model.MaterialSale.PaymentMethod;
import com.moriba.skultem.domain.model.MaterialSale.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "material_sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialSaleEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    // Nullable - a walk-in buyer is recorded via customerName instead.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = true)
    private StudentEntity student;

    private String customerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private MaterialEntity material;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private Instant fulfilledAt;

    // Nullable only for sales recorded before this column existed - every new sale always creates
    // and links one. See Supply#sourceSaleId for the other direction of this link.
    private String supplyId;

    private Instant createdAt;
    private Instant updatedAt;
}
