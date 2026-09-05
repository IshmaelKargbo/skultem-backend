package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.PayComponent;

import lombok.Getter;

// A reusable starting point for a teacher's SalaryStructure - "Grade A Teacher", "Support Staff",
// etc - so an admin can configure a basic salary plus a named set of allowances/deductions once
// and reuse it, instead of re-typing the same lines for every teacher on that grade. Applying a
// template to a teacher (see SetSalaryStructureUseCase) copies its lines onto that teacher's
// SalaryStructure at that moment; editing the template afterwards never reaches back into
// structures already built from it, same "snapshot, don't link" rule as Payslip vs SalaryStructure.
@Getter
public class SalaryTemplate extends AggregateRoot<String> {

    private String schoolId;
    private String name;
    private BigDecimal basicSalary;
    private List<PayComponent> allowances;
    private List<PayComponent> deductions;

    public SalaryTemplate(
            String id,
            String schoolId,
            String name,
            BigDecimal basicSalary,
            List<PayComponent> allowances,
            List<PayComponent> deductions,
            Instant createdAt,
            Instant updatedAt) {

        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.basicSalary = basicSalary;
        this.allowances = allowances;
        this.deductions = deductions;

        touch(updatedAt);
    }

    public static SalaryTemplate create(String id, String schoolId, String name, BigDecimal basicSalary,
            List<PayComponent> allowances, List<PayComponent> deductions) {

        Instant now = Instant.now();

        return new SalaryTemplate(id, schoolId, name, basicSalary, allowances, deductions, now, now);
    }

    public void update(String name, BigDecimal basicSalary, List<PayComponent> allowances,
            List<PayComponent> deductions) {

        this.name = name;
        this.basicSalary = basicSalary;
        this.allowances = allowances;
        this.deductions = deductions;

        touch(Instant.now());
    }

    public BigDecimal totalAllowances() {
        return allowances.stream().map(a -> a.resolve(basicSalary)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalDeductions() {
        return deductions.stream().map(d -> d.resolve(basicSalary)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal grossSalary() {
        return basicSalary.add(totalAllowances());
    }

    public BigDecimal netSalary() {
        return grossSalary().subtract(totalDeductions());
    }
}
