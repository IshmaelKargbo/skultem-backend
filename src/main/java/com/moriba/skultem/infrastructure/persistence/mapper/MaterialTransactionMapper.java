package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.MaterialTransaction;
import com.moriba.skultem.infrastructure.persistence.entity.MaterialTransactionEntity;

public class MaterialTransactionMapper {
    public static MaterialTransaction toDomain(MaterialTransactionEntity param) {
        if (param == null)
            return null;
        var material = MaterialMapper.toDomain(param.getMaterial());
        return new MaterialTransaction(param.getId(), param.getSchoolId(), material, param.getQty(), param.getDirection(), param.getNote(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static MaterialTransactionEntity toEntity(MaterialTransaction param) {
        if (param == null)
            return null;
        var material = MaterialMapper.toEntity(param.getMaterial());
        return MaterialTransactionEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .material(material)
                .direction(param.getDirection())
                .note(param.getNote())
                .qty(param.getQty())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
