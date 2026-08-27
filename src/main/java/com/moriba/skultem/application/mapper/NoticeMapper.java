package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.NoticeDTO;
import com.moriba.skultem.domain.model.Notice;

public class NoticeMapper {
    public static NoticeDTO toDTO(Notice param) {
        return new NoticeDTO(
                param.getId(),
                param.getTitle(),
                param.getContent(),
                param.getCategory(),
                param.getAudience(),
                param.isPinned(),
                param.getPostedByName(),
                param.getCreatedAt(),
                param.getExpiresAt());
    }
}
