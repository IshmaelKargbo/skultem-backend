package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Supply;
import com.moriba.skultem.infrastructure.persistence.entity.SupplyEntity;

public class SupplyMapper {
    public static Supply toDomain(SupplyEntity param) {
        if (param == null) return null;

        var student = StudentMapper.toDomain(param.getStudent());
        var material = MaterialMapper.toDomain(param.getMaterial());
        return new Supply(param.getId(), param.getSchoolId(), student, material, param.getQty(),
                param.getCollectedQty(), param.getStatus(), param.getCollectedOn(), param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static SupplyEntity toEntity(Supply param) {
        if (param == null) return null;
        var student = StudentMapper.toEntity(param.getStudent());
        var material = MaterialMapper.toEntity(param.getMaterial());

        return SupplyEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .student(student)
                .material(material)
                .collectedOn(param.getCollectedOn())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
