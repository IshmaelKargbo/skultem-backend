package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.PayComponent;
import lombok.Getter;

// The compensation currently in effect for a teacher. Payroll runs snapshot these values into a
// Payslip at creation time, so editing a SalaryStructure only affects future runs.
//
// Allowances/deductions are a named, itemized list (each a PayComponent - fixed amount or % of
// basicSalary) rather than one lump sum each, so a payslip can actually explain what it's paying
// for ("Housing Allowance", "Transport", "Tax", ...) instead of just two opaque totals. templateId
// records which SalaryTemplate (if any) this was built from, purely for display/traceability - the
// lines themselves are copied in at that point, not kept linked, so editing the template later
// never silently changes a teacher's existing pay.
@Getter
public class SalaryStructure extends AggregateRoot<String> {

    private String schoolId;
    private Teacher teacher;
    private String templateId;
    private String templateName;
    private BigDecimal basicSalary;
    private List<PayComponent> allowances;
    private List<PayComponent> deductions;

    public SalaryStructure(
            String id,
            String schoolId,
            Teacher teacher,
            String templateId,
            String templateName,
            BigDecimal basicSalary,
            List<PayComponent> allowances,
            List<PayComponent> deductions,
            Instant createdAt,
            Instant updatedAt) {

        super(id, createdAt);
        this.schoolId = schoolId;
        this.teacher = teacher;
        this.templateId = templateId;
        this.templateName = templateName;
        this.basicSalary = basicSalary;
        this.allowances = allowances;
        this.deductions = deductions;

        touch(updatedAt);
    }

    public static SalaryStructure create(
            String id,
            String schoolId,
            Teacher teacher,
            String templateId,
            String templateName,
            BigDecimal basicSalary,
            List<PayComponent> allowances,
            List<PayComponent> deductions) {

        Instant now = Instant.now();

        return new SalaryStructure(id, schoolId, teacher, templateId, templateName, basicSalary, allowances,
                deductions, now, now);
    }

    public void update(String templateId, String templateName, BigDecimal basicSalary,
            List<PayComponent> allowances, List<PayComponent> deductions) {

        this.templateId = templateId;
        this.templateName = templateName;
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
