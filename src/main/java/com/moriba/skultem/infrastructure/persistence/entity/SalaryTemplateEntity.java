package com.moriba.skultem.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "salary_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryTemplateEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal basicSalary;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "salary_template_allowances", joinColumns = @JoinColumn(name = "salary_template_id"))
    @Builder.Default
    private List<PayComponentEmbeddable> allowances = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "salary_template_deductions", joinColumns = @JoinColumn(name = "salary_template_id"))
    @Builder.Default
    private List<PayComponentEmbeddable> deductions = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
}
