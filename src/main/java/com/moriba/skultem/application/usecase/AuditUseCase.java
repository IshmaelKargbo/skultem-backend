package com.moriba.skultem.application.usecase;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.audit.AuditEvent;
import com.moriba.skultem.domain.model.AuditLog.Status;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditUseCase {

    private final ApplicationEventPublisher eventPublisher;
    private final HttpServletRequest request;

    public void log(
            String action,
            String userId,
            String schoolId,
            Status status,
            String details) {

        eventPublisher.publishEvent(new AuditEvent(
                action,
                schoolId,
                userId,
                getClientIp(),
                getUserAgent(),
                status,
                details
        ));
    }

    public void logSystem(
            String action,
            String userId,
            Status status,
            String details) {

        eventPublisher.publishEvent(new AuditEvent(
                action,
                null,
                userId,
                getClientIp(),
                getUserAgent(),
                status,
                details
        ));
    }

    private String getUserAgent() {
        String ua = request.getHeader("User-Agent");
        return (ua == null || ua.isBlank()) ? "Unknown" : ua;
    }

    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}