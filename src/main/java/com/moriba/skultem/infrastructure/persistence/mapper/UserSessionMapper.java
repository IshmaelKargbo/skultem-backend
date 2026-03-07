package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.UserSession;
import com.moriba.skultem.infrastructure.persistence.entity.UserSessionEntity;

public class UserSessionMapper {
    public static UserSession toDomain(UserSessionEntity param) {
        var user = UserMapper.toDomain(param.getUser());
        return new UserSession(param.getId(), param.getSchoolId(), user, param.getIpAddress(), param.getDevice(),
                param.getDeviceType(), param.getOs(), param.getBrowser(), param.getUserAgent(), param.isActive(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static UserSessionEntity toEntity(UserSession param) {
        var user = UserMapper.toEntity(param.getUser());

        return UserSessionEntity.builder()
                .id(param.getId())
                .browser(param.getBrowser())
                .device(param.getDevice())
                .user(user)
                .deviceType(param.getDeviceType())
                .ipAddress(param.getIpAddress())
                .userAgent(param.getUserAgent())
                .schoolId(param.getSchoolId())
                .active(param.isActive())
                .os(param.getOs())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
