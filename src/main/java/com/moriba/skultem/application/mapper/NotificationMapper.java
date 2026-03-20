package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.NotificationDTO;
import com.moriba.skultem.domain.model.Notification;

public class NotificationMapper {
    public static NotificationDTO toDTO(Notification param) {
        return new NotificationDTO(param.getId(), param.getSchoolId(), param.getTitle(), param.getMessage(),
                param.getMeta(), param.getType(), param.getPriority(), param.isRead(), param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
