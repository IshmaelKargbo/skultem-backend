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
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
