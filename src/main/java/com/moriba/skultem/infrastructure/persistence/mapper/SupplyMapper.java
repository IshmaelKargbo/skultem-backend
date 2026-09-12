package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Supply;
import com.moriba.skultem.infrastructure.persistence.entity.SupplyEntity;

public class SupplyMapper {
    public static Supply toDomain(SupplyEntity param) {
        if (param == null) return null;

        var student = param.getStudent() != null ? StudentMapper.toDomain(param.getStudent()) : null;
        var material = MaterialMapper.toDomain(param.getMaterial());
        return new Supply(param.getId(), param.getSchoolId(), student, param.getCustomerName(), material,
                param.getQty(), param.getCollectedQty(), param.getStatus(), param.getCollectedOn(),
                param.getSourceSaleId(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static SupplyEntity toEntity(Supply param) {
        if (param == null) return null;
        var student = param.getStudent() != null ? StudentMapper.toEntity(param.getStudent()) : null;
        var material = MaterialMapper.toEntity(param.getMaterial());

        return SupplyEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .student(student)
                .customerName(param.getCustomerName())
                .qty(param.getQty())
                .collectedQty(param.getCollectedQty())
                .material(material)
                .sourceSaleId(param.getSourceSaleId())
                .collectedOn(param.getCollectedOn())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .status(param.getStatus())
                .build();
    }
}
