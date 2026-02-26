package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.FeeCategory;
import com.moriba.skultem.infrastructure.persistence.entity.FeeCategoryEntity;

public class FeeCategoryMapper {
    public static FeeCategory toDomain(FeeCategoryEntity param) {
        return new FeeCategory(param.getId(), param.getSchoolId(), param.getName(), param.getDscription(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static FeeCategoryEntity toEntity(FeeCategory args) {
        return FeeCategoryEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .name(args.getName())
                .dscription(args.getDescription())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
