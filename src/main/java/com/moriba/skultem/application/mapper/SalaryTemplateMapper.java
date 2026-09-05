package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.SalaryTemplateDTO;
import com.moriba.skultem.domain.model.SalaryTemplate;

public class SalaryTemplateMapper {
    public static SalaryTemplateDTO toDTO(SalaryTemplate param) {
        if (param == null) {
            return null;
        }

        return new SalaryTemplateDTO(
                param.getId(),
                param.getSchoolId(),
                param.getName(),
                param.getBasicSalary(),
                SalaryStructureMapper.toDTOItems(param.getAllowances(), param.getBasicSalary()),
                SalaryStructureMapper.toDTOItems(param.getDeductions(), param.getBasicSalary()),
                param.totalAllowances(),
                param.totalDeductions(),
                param.grossSalary(),
                param.netSalary(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
