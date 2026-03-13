package com.moriba.skultem.application.dto;

import java.time.Instant;

public record UserSessionDTO(
        String id,
        String userId,
        String userName,
        String userEmail,
        String ipAddress,
        String device,
        String deviceType,
        String os,
        String browser,
        String userAgent,
        boolean active,
        Instant createdAt) {
}
