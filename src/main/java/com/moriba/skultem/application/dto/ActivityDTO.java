package com.moriba.skultem.application.dto;

import java.time.Instant;

import com.moriba.skultem.domain.vo.ActivityType;

public record ActivityDTO(
        String id,
        String schoolId,
        ActivityType type,
        String title,
        String subject,
        String meta,
        String referenceId,
        Instant createdAt) {
}
