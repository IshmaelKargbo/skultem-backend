package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.FeeCategory;
import com.moriba.skultem.infrastructure.persistence.entity.FeeCategoryEntity;

public class FeeCategoryMapper {
    public static FeeCategory toDomain(FeeCategoryEntity param) {
        if (param == null) return null;
        return new FeeCategory(param.getId(), param.getSchoolId(), param.getName(), param.getDscription(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static FeeCategoryEntity toEntity(FeeCategory param) {
        if (param == null) return null;
        return FeeCategoryEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .dscription(param.getDescription())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
