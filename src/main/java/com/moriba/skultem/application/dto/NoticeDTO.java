package com.moriba.skultem.application.dto;

import java.time.Instant;

import com.moriba.skultem.domain.model.Notice.Category;
import com.moriba.skultem.domain.vo.Audience;

public record NoticeDTO(
        String id,
        String title,
        String content,
        Category category,
        Audience audience,
        boolean pinned,
        String postedBy,
        Instant postedAt,
        Instant expiresAt) {
}
