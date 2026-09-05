package com.moriba.skultem.infrastructure.persistence.entity;

import java.math.BigDecimal;

import com.moriba.skultem.domain.vo.PayComponentType;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// One allowance/deduction line, embedded via @ElementCollection into
// salary_template_allowances/salary_template_deductions and
// salary_structure_allowances/salary_structure_deductions - see PayComponent (its domain
// equivalent) for why this needs no identity of its own; the whole list is replaced wholesale on
// every save, so there's nothing an individual line needs to be keyed on across edits.
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayComponentEmbeddable {
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayComponentType type;

    @Column(nullable = false)
    private BigDecimal value;
}
