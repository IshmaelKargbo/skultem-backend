package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.AssessmentTemplate;
import com.moriba.skultem.infrastructure.persistence.entity.AssessmentTemplateEntity;

public class AssessmentTemplateMapper {
    public static AssessmentTemplate toDomain(AssessmentTemplateEntity param) {
        if (param == null) {
            return null;
        }

        return new AssessmentTemplate(
                param.getId(),
                param.getSchoolId(),
                param.getName(),
                param.getDescription(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static AssessmentTemplateEntity toEntity(AssessmentTemplate param) {
        if (param == null) {
            return null;
        }

        return AssessmentTemplateEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .description(param.getDescription())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
