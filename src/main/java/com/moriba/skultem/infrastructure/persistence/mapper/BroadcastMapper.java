package com.moriba.skultem.infrastructure.persistence.mapper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.moriba.skultem.domain.model.Broadcast;
import com.moriba.skultem.infrastructure.persistence.entity.BroadcastEntity;

public class BroadcastMapper {
    public static Broadcast toDomain(BroadcastEntity param) {
        if (param == null) return null;

        List<Broadcast.Channel> channels = Arrays.stream(param.getChannels().split(","))
                .filter(c -> !c.isBlank())
                .map(Broadcast.Channel::valueOf)
                .collect(Collectors.toList());

        return new Broadcast(param.getId(), param.getSchoolId(), param.getTitle(), param.getMessage(),
                param.getAudience(), channels, param.getStatus(), param.getRecipientsCount(),
                param.getDeliveredCount(), param.getSentByUserId(), param.getSentByName(), param.getScheduledAt(),
                param.getSentAt(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static BroadcastEntity toEntity(Broadcast param) {
        if (param == null) return null;

        String channels = param.getChannels().stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));

        return BroadcastEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .title(param.getTitle())
                .message(param.getMessage())
                .audience(param.getAudience())
                .channels(channels)
                .status(param.getStatus())
                .recipientsCount(param.getRecipientsCount())
                .deliveredCount(param.getDeliveredCount())
                .sentByUserId(param.getSentByUserId())
                .sentByName(param.getSentByName())
                .scheduledAt(param.getScheduledAt())
                .sentAt(param.getSentAt())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
