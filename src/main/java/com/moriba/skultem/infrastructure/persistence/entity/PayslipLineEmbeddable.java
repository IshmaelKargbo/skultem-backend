package com.moriba.skultem.infrastructure.persistence.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// See PayslipLine - payslip_allowance_lines / payslip_deduction_lines.
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayslipLineEmbeddable {
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal amount;
}
