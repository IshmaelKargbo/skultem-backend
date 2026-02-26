package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.infrastructure.persistence.entity.SubjectEntity;

public class SubjectMapper {
    public static Subject toDomain(SubjectEntity param) {
        return new Subject(param.getId(), param.getSchoolId(), param.getName(), param.getCode(), param.getDescription(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static SubjectEntity toEntity(Subject param) {
        return SubjectEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .code(param.getCode())
                .description(param.getDescription())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
