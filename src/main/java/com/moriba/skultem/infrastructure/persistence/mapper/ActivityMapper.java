package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Activity;
import com.moriba.skultem.infrastructure.persistence.entity.ActivityEntity;

public class ActivityMapper {
    public static Activity toDomain(ActivityEntity param) {
        return new Activity(
                param.getId(),
                param.getSchoolId(),
                param.getType(),
                param.getTitle(),
                param.getSubject(),
                param.getMeta(),
                param.getReferenceId(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static ActivityEntity toEntity(Activity param) {
        return ActivityEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .type(param.getType())
                .title(param.getTitle())
                .subject(param.getSubject())
                .meta(param.getMeta())
                .referenceId(param.getReferenceId())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
