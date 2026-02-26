package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.infrastructure.persistence.entity.SectionEntity;

public class SectionMapper {
    public static Section toDomain(SectionEntity param) {
        return new Section(param.getId(), param.getSchoolId(), param.getName(), param.getDescription(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static SectionEntity toEntity(Section args) {
        return SectionEntity.builder()
                .id(args.getId())
                .name(args.getName())
                .schoolId(args.getSchoolId())
                .description(args.getDescription())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
