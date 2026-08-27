package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Notice;
import com.moriba.skultem.infrastructure.persistence.entity.NoticeEntity;

public class NoticeMapper {
    public static Notice toDomain(NoticeEntity param) {
        if (param == null) return null;

        return new Notice(param.getId(), param.getSchoolId(), param.getTitle(), param.getContent(),
                param.getCategory(), param.getAudience(), param.isPinned(), param.getPostedByUserId(),
                param.getPostedByName(), param.getExpiresAt(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static NoticeEntity toEntity(Notice param) {
        if (param == null) return null;

        return NoticeEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .title(param.getTitle())
                .content(param.getContent())
                .category(param.getCategory())
                .audience(param.getAudience())
                .pinned(param.isPinned())
                .postedByUserId(param.getPostedByUserId())
                .postedByName(param.getPostedByName())
                .expiresAt(param.getExpiresAt())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
