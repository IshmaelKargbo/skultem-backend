package com.moriba.skultem.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moriba.skultem.domain.model.Notification;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.infrastructure.persistence.entity.NotificationEntity;

import java.util.Collections;
import java.util.Map;

public class NotificationMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static Notification toDomain(NotificationEntity entity) {
        User user = UserMapper.toDomain(entity.getOwner());

        Map<String, String> meta;
        try {
            meta = OBJECT_MAPPER.readValue(entity.getMeta(), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            meta = Collections.emptyMap();
        }

        return new Notification(
                entity.getId(),
                entity.getSchoolId(),
                user,
                entity.getType(),
                entity.getTitle(),
                entity.getMessage(),
                meta,
                entity.getPriority(),
                entity.isRead(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static NotificationEntity toEntity(Notification domain) {
        String metaJson;
        try {
            metaJson = OBJECT_MAPPER.writeValueAsString(domain.getMeta());
        } catch (Exception e) {
            metaJson = "{}";
        }

        return NotificationEntity.builder()
                .id(domain.getId())
                .schoolId(domain.getSchoolId())
                .owner(UserMapper.toEntity(domain.getOwner()))
                .type(domain.getType())
                .title(domain.getTitle())
                .message(domain.getMessage())
                .priority(domain.getPriority())
                .meta(metaJson)
                .read(domain.isRead())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}