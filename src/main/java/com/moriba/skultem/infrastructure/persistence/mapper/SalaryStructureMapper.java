package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.SalaryStructure;
import com.moriba.skultem.infrastructure.persistence.entity.SalaryStructureEntity;

public class SalaryStructureMapper {
    public static SalaryStructure toDomain(SalaryStructureEntity param) {
        if (param == null) {
            return null;
        }

        return new SalaryStructure(
                param.getId(),
                param.getSchoolId(),
                TeacherMapper.toDomain(param.getTeacher()),
                param.getBasicSalary(),
                param.getAllowances(),
                param.getDeductions(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static SalaryStructureEntity toEntity(SalaryStructure param) {
        if (param == null) {
            return null;
        }

        return SalaryStructureEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .teacher(TeacherMapper.toEntity(param.getTeacher()))
                .basicSalary(param.getBasicSalary())
                .allowances(param.getAllowances())
                .deductions(param.getDeductions())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
