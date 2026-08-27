package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.BroadcastDTO;
import com.moriba.skultem.domain.model.Broadcast;

public class BroadcastMapper {
    public static BroadcastDTO toDTO(Broadcast param) {
        return new BroadcastDTO(
                param.getId(),
                param.getTitle(),
                param.getMessage(),
                param.getAudience(),
                param.getChannels(),
                param.getStatus(),
                param.getRecipientsCount(),
                param.getDeliveredCount(),
                param.getSentByName(),
                param.getScheduledAt(),
                param.getSentAt(),
                param.getCreatedAt());
    }
}
