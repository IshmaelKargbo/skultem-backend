package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.SchoolLevel;
import com.moriba.skultem.infrastructure.persistence.entity.SchoolLevelEntity;

public class SchoolLevelMapper {
    public static SchoolLevel toDomain(SchoolLevelEntity param) {
        return new SchoolLevel(param.getId(), param.getSchoolId(), param.getLevel(), param.getManagementSectionId(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static SchoolLevelEntity toEntity(SchoolLevel args) {
        return SchoolLevelEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .level(args.getLevel())
                .managementSectionId(args.getManagementSectionId())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
