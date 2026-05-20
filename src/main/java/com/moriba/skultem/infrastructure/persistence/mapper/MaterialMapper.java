package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Material;
import com.moriba.skultem.infrastructure.persistence.entity.MaterialEntity;

public class MaterialMapper {
    public static Material toDomain(MaterialEntity param) {
        return new Material(param.getId(), param.getSchoolId(), param.getName(), param.getUnit(),
                MaterialCategoryMapper.toDomain(param.getCategory()), param.getStockQuantity(), param.getReorderLevel(),
                param.getLastRestockedAt(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static MaterialEntity toEntity(Material param) {
        return MaterialEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .unit(param.getUnit())
                .category(MaterialCategoryMapper.toEntity(param.getCategory()))
                .lastRestockedAt(param.getLastRestockedAt())
                .reorderLevel(param.getReorderLevel())
                .stockQuantity(param.getStockQuantity())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
