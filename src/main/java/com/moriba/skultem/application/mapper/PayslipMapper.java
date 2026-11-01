package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.PayslipDTO;
import com.moriba.skultem.domain.model.PayrollRun;
import com.moriba.skultem.domain.model.Payslip;

public class PayslipMapper {
    public static PayslipDTO toDTO(Payslip param, PayrollRun run) {
        if (param == null) {
            return null;
        }

        return new PayslipDTO(
                param.getId(),
                param.getSchoolId(),
                param.getPayrollRunId(),
                run != null ? run.getPeriod() : null,
                run != null ? run.getPayDate() : null,
                TeacherMapper.toDTO(param.getTeacher()),
                param.getBasicSalary(),
                param.getAllowances(),
                param.getDeductions(),
                param.grossSalary(),
                param.netSalary(),
                param.isIncluded(),
                param.getAllowanceLines().stream()
                        .map(l -> new com.moriba.skultem.application.dto.PayslipLineDTO(l.name(), l.amount())).toList(),
                param.getDeductionLines().stream()
                        .map(l -> new com.moriba.skultem.application.dto.PayslipLineDTO(l.name(), l.amount())).toList(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
