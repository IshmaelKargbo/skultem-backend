package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Behaviour;
import com.moriba.skultem.domain.model.BehaviourCategory;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.infrastructure.persistence.entity.BehaviourCategoryEntity;
import com.moriba.skultem.infrastructure.persistence.entity.BehaviourEntity;
import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity;

public class BehaviourMapper {
    public static Behaviour toDomain(BehaviourEntity param) {
        if (param == null) {
            return null;
        }

        BehaviourCategory category = BehaviourCategoryMapper.toDomain(param.getCategory());
        Enrollment enrollment = EnrollmentMapper.toDomain(param.getEnrollment());

        return new Behaviour(param.getId(), param.getSchoolId(), enrollment, param.getKind(), category, param.getCreatedAt(), param.getUpdatedAt());
    }

    public static BehaviourEntity toEntity(Behaviour param) {
        if (param == null) {
            return null;
        }

        BehaviourCategoryEntity category = BehaviourCategoryMapper.toEntity(param.getCategory());
        EnrollmentEntity enrollment = EnrollmentMapper.toEntity(param.getEnrollment());

        return BehaviourEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .category(category)
                .enrollment(enrollment)
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
