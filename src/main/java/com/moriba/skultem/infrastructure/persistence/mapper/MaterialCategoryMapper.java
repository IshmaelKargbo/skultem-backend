package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.MaterialCategory;
import com.moriba.skultem.infrastructure.persistence.entity.MaterialCategoryEntity;

public class MaterialCategoryMapper {
    public static MaterialCategory toDomain(MaterialCategoryEntity param) {
        if (param == null) return null;

        return new MaterialCategory(param.getId(), param.getSchoolId(), param.getName(), param.getDescription(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static MaterialCategoryEntity toEntity(MaterialCategory param) {
        if (param == null) return null;

        return MaterialCategoryEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .description(param.getDescription())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
