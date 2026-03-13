package com.moriba.skultem.application.dto;

import java.time.Instant;

import com.moriba.skultem.domain.model.AuditLog.Status;

public record AuditLogDTO(
        String id,
        String action,
        String academicYearName,
        String userId,
        String userName,
        String userEmail,
        String ipAddress,
        String device,
        String deviceType,
        String os,
        String browser,
        Status status,
        String details,
        Instant createdAt) {
}
