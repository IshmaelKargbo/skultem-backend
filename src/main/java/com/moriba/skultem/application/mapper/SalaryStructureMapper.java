package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.SalaryStructureDTO;
import com.moriba.skultem.domain.model.SalaryStructure;

public class SalaryStructureMapper {
    public static SalaryStructureDTO toDTO(SalaryStructure param) {
        if (param == null) {
            return null;
        }

        return new SalaryStructureDTO(
                param.getId(),
                param.getSchoolId(),
                TeacherMapper.toDTO(param.getTeacher()),
                param.getBasicSalary(),
                param.getAllowances(),
                param.getDeductions(),
                param.grossSalary(),
                param.netSalary(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
