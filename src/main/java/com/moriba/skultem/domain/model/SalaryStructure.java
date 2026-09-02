package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.Getter;

// The compensation currently in effect for a teacher. Payroll runs snapshot these values into a
// Payslip at creation time, so editing a SalaryStructure only affects future runs.
@Getter
public class SalaryStructure extends AggregateRoot<String> {

    private String schoolId;
    private Teacher teacher;
    private BigDecimal basicSalary;
    private BigDecimal allowances;
    private BigDecimal deductions;

    public SalaryStructure(
            String id,
            String schoolId,
            Teacher teacher,
            BigDecimal basicSalary,
            BigDecimal allowances,
            BigDecimal deductions,
            Instant createdAt,
            Instant updatedAt) {

        super(id, createdAt);
        this.schoolId = schoolId;
        this.teacher = teacher;
        this.basicSalary = basicSalary;
        this.allowances = allowances;
        this.deductions = deductions;

        touch(updatedAt);
    }

    public static SalaryStructure create(
            String id,
            String schoolId,
            Teacher teacher,
            BigDecimal basicSalary,
            BigDecimal allowances,
            BigDecimal deductions) {

        Instant now = Instant.now();

        return new SalaryStructure(id, schoolId, teacher, basicSalary, allowances, deductions, now, now);
    }

    public void update(BigDecimal basicSalary, BigDecimal allowances, BigDecimal deductions) {
        this.basicSalary = basicSalary;
        this.allowances = allowances;
        this.deductions = deductions;

        touch(Instant.now());
    }

    public BigDecimal grossSalary() {
        return basicSalary.add(allowances);
    }

    public BigDecimal netSalary() {
        return grossSalary().subtract(deductions);
    }
}
