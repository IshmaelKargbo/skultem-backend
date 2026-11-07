package com.moriba.skultem.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// One row of fee_structure_installments (its position is the list index).
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeInstallmentEmbeddable {
    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
}
