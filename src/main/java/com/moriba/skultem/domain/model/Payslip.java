package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.PayslipLine;

import lombok.Getter;

// A frozen snapshot of one teacher's compensation within a single PayrollRun. Values are copied
// from SalaryStructure when the run is created and never change afterwards, even if the teacher's
// SalaryStructure is later edited - payroll history has to stay accurate.
@Getter
public class Payslip extends AggregateRoot<String> {

    private String schoolId;
    private String payrollRunId;
    private Teacher teacher;
    private BigDecimal basicSalary;
    private BigDecimal allowances;
    private BigDecimal deductions;
    private boolean included;
    // Frozen copies of each line (empty for payslips created before lines were kept).
    private List<PayslipLine> allowanceLines;
    private List<PayslipLine> deductionLines;

    public Payslip(
            String id,
            String schoolId,
            String payrollRunId,
            Teacher teacher,
            BigDecimal basicSalary,
            BigDecimal allowances,
            BigDecimal deductions,
            boolean included,
            List<PayslipLine> allowanceLines,
            List<PayslipLine> deductionLines,
            Instant createdAt,
            Instant updatedAt) {

        super(id, createdAt);
        this.allowanceLines = allowanceLines == null ? List.of() : allowanceLines;
        this.deductionLines = deductionLines == null ? List.of() : deductionLines;
        this.schoolId = schoolId;
        this.payrollRunId = payrollRunId;
        this.teacher = teacher;
        this.basicSalary = basicSalary;
        this.allowances = allowances;
        this.deductions = deductions;
        this.included = included;

        touch(updatedAt);
    }

    public static Payslip fromSalaryStructure(String id, PayrollRun run, SalaryStructure structure) {
        Instant now = Instant.now();

        // Allowances/deductions are itemized on SalaryStructure (see PayComponent) but a Payslip
        // keeps its historical snapshot flat, same as before this changed - resolving the lines
        // down to totals here is enough to keep payroll history accurate without also needing to
        // freeze a full item breakdown per past payslip.
        return new Payslip(
                id,
                structure.getSchoolId(),
                run.getId(),
                structure.getTeacher(),
                structure.getBasicSalary(),
                structure.totalAllowances(),
                structure.totalDeductions(),
                true,
                lines(structure.getAllowances(), structure.getBasicSalary()),
                lines(structure.getDeductions(), structure.getBasicSalary()),
                now,
                now);
    }

    private static List<PayslipLine> lines(List<com.moriba.skultem.domain.vo.PayComponent> items,
            BigDecimal basicSalary) {
        if (items == null) return List.of();
        return items.stream().map(i -> new PayslipLine(i.name(), i.resolve(basicSalary))).toList();
    }

    public void setIncluded(boolean included, PayrollRun run) {
        if (!run.isDraft()) {
            throw new BadRequestException("Employees can only be included or excluded while the run is a draft");
        }

        this.included = included;
        touch(Instant.now());
    }

    public BigDecimal grossSalary() {
        return basicSalary.add(allowances);
    }

    public BigDecimal netSalary() {
        return grossSalary().subtract(deductions);
    }
}
