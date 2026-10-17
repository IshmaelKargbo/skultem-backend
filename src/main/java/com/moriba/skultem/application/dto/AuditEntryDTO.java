package com.moriba.skultem.application.dto;

import java.time.Instant;

// One recorded action, as the system admin's audit log shows it. device and ip are included for investigating who did
// what; details is the summary the action itself recorded.
public record AuditEntryDTO(String id, String action, String status, String details, String schoolId,
        String schoolName, String userId, String userName, String userEmail, String ipAddress, String device,
        String os, String browser, Instant createdAt) {
}
