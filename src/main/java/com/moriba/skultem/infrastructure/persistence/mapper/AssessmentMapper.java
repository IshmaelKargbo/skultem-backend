package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Assessment;
import com.moriba.skultem.domain.model.AssessmentTemplate;
import com.moriba.skultem.infrastructure.persistence.entity.AssessmentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.AssessmentTemplateEntity;

public class AssessmentMapper {
    public static Assessment toDomain(AssessmentEntity param) {
        if (param == null) {
            return null;
        }

        AssessmentTemplate template = null;
        if (param.getTemplate() != null) {
            template = AssessmentTemplateMapper.toDomain(param.getTemplate());
        }

        return new Assessment(
                param.getId(),
                param.getSchoolId(),
                template,
                param.getName(),
                param.getWeight(),
                param.getPosition(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static AssessmentEntity toEntity(Assessment param) {
        if (param == null) {
            return null;
        }

        AssessmentTemplateEntity template = null;
        if (param.getTemplate() != null) {
            template = AssessmentTemplateMapper.toEntity(param.getTemplate());
        }

        return AssessmentEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .template(template)
                .name(param.getName())
                .weight(param.getWeight())
                .position(param.getPosition())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
