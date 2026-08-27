package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

import com.moriba.skultem.domain.model.Broadcast.Channel;
import com.moriba.skultem.domain.model.Broadcast.Status;
import com.moriba.skultem.domain.vo.Audience;

public record BroadcastDTO(
        String id,
        String title,
        String message,
        Audience audience,
        List<Channel> channels,
        Status status,
        int recipientsCount,
        int deliveredCount,
        String sentBy,
        Instant scheduledAt,
        Instant sentAt,
        Instant createdAt) {
}
