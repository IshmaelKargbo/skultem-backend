package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.BehaviourCategory;
import com.moriba.skultem.infrastructure.persistence.entity.BehaviourCategoryEntity;

public class BehaviourCategoryMapper {
    public static BehaviourCategory toDomain(BehaviourCategoryEntity param) {
        if (param == null) {
            return null;
        }

        return new BehaviourCategory(param.getId(), param.getSchoolId(), param.getName(), param.getDescription(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static BehaviourCategoryEntity toEntity(BehaviourCategory param) {
        if (param == null) {
            return null;
        }

        return BehaviourCategoryEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .description(param.getDescription())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
