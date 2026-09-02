package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.domain.shared.AggregateRoot;
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

    public Payslip(
            String id,
            String schoolId,
            String payrollRunId,
            Teacher teacher,
            BigDecimal basicSalary,
            BigDecimal allowances,
            BigDecimal deductions,
            boolean included,
            Instant createdAt,
            Instant updatedAt) {

        super(id, createdAt);
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

        return new Payslip(
                id,
                structure.getSchoolId(),
                run.getId(),
                structure.getTeacher(),
                structure.getBasicSalary(),
                structure.getAllowances(),
                structure.getDeductions(),
                true,
                now,
                now);
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
