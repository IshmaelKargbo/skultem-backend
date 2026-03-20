package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.Map;

import com.moriba.skultem.domain.model.Notification.Type;
import com.moriba.skultem.domain.vo.Priority;

public record NotificationDTO(String id, String schoolId, String title, String message, Map<String, String> meta,
                Type type, Priority priority, boolean read, Instant createdAt, Instant updatedAt) {
}
