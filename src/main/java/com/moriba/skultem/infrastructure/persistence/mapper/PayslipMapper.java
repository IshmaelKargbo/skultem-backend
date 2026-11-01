package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Payslip;
import com.moriba.skultem.infrastructure.persistence.entity.PayslipEntity;

public class PayslipMapper {
    public static Payslip toDomain(PayslipEntity param) {
        if (param == null) {
            return null;
        }

        return new Payslip(
                param.getId(),
                param.getSchoolId(),
                param.getPayrollRunId(),
                TeacherMapper.toDomain(param.getTeacher()),
                param.getBasicSalary(),
                param.getAllowances(),
                param.getDeductions(),
                param.isIncluded(),
                toDomainLines(param.getAllowanceLines()),
                toDomainLines(param.getDeductionLines()),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static PayslipEntity toEntity(Payslip param) {
        if (param == null) {
            return null;
        }

        return PayslipEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .payrollRunId(param.getPayrollRunId())
                .teacher(TeacherMapper.toEntity(param.getTeacher()))
                .basicSalary(param.getBasicSalary())
                .allowances(param.getAllowances())
                .deductions(param.getDeductions())
                .included(param.isIncluded())
                // Always carried through - saving without them (e.g. include/exclude) would wipe them.
                .allowanceLines(toEntityLines(param.getAllowanceLines()))
                .deductionLines(toEntityLines(param.getDeductionLines()))
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }

    private static java.util.List<com.moriba.skultem.domain.vo.PayslipLine> toDomainLines(
            java.util.List<com.moriba.skultem.infrastructure.persistence.entity.PayslipLineEmbeddable> lines) {
        if (lines == null) return java.util.List.of();
        return lines.stream()
                .map(l -> new com.moriba.skultem.domain.vo.PayslipLine(l.getName(), l.getAmount()))
                .toList();
    }

    private static java.util.List<com.moriba.skultem.infrastructure.persistence.entity.PayslipLineEmbeddable> toEntityLines(
            java.util.List<com.moriba.skultem.domain.vo.PayslipLine> lines) {
        var out = new java.util.ArrayList<com.moriba.skultem.infrastructure.persistence.entity.PayslipLineEmbeddable>();
        if (lines != null) {
            lines.forEach(l -> out.add(
                    new com.moriba.skultem.infrastructure.persistence.entity.PayslipLineEmbeddable(l.name(), l.amount())));
        }
        return out;
    }
}
