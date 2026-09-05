package com.moriba.skultem.application.mapper;

import java.math.BigDecimal;
import java.util.List;

import com.moriba.skultem.application.dto.PayComponentDTO;
import com.moriba.skultem.application.dto.SalaryStructureDTO;
import com.moriba.skultem.domain.model.SalaryStructure;
import com.moriba.skultem.domain.vo.PayComponent;

public class SalaryStructureMapper {
    public static SalaryStructureDTO toDTO(SalaryStructure param) {
        if (param == null) {
            return null;
        }

        return new SalaryStructureDTO(
                param.getId(),
                param.getSchoolId(),
                TeacherMapper.toDTO(param.getTeacher()),
                param.getTemplateId(),
                param.getTemplateName(),
                param.getBasicSalary(),
                toDTOItems(param.getAllowances(), param.getBasicSalary()),
                toDTOItems(param.getDeductions(), param.getBasicSalary()),
                param.totalAllowances(),
                param.totalDeductions(),
                param.grossSalary(),
                param.netSalary(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    static List<PayComponentDTO> toDTOItems(List<PayComponent> items, BigDecimal basicSalary) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(item -> new PayComponentDTO(item.name(), item.type().name(), item.value(),
                        item.resolve(basicSalary)))
                .toList();
    }
}
