package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.PromotionConfig;
import com.moriba.skultem.infrastructure.persistence.entity.PromotionConfigEntity;

public class PromotionConfigMapper {
    public static PromotionConfig toDomain(PromotionConfigEntity param) {
        if (param == null) {
            return null;
        }

        return new PromotionConfig(param.getId(), param.getSchoolId(), param.getMinPassMark(),
                param.getMaxRepeatCount(), param.isRequireApproval(), param.isRequireRemarkForPromote(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static PromotionConfigEntity toEntity(PromotionConfig param) {
        return PromotionConfigEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .minPassMark(param.getMinPassMark())
                .maxRepeatCount(param.getMaxRepeatCount())
                .requireApproval(param.isRequireApproval())
                .requireRemarkForPromote(param.isRequireRemarkForPromote())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
