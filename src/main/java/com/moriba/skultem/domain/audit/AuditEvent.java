package com.moriba.skultem.domain.audit;

import com.moriba.skultem.domain.model.AuditLog.Status;

public record AuditEvent(
        String action,
        String schoolId,
        String userId,
        String ip,
        String userAgent,
        Status status,
        String details
) {}