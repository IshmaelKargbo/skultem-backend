package com.moriba.skultem.application.dto;

import java.time.Instant;

public record PlatformAnnouncementDTO(String id, String title, String message, String severity, boolean active,
        Instant startsAt, Instant endsAt, boolean live, Instant createdAt, Instant updatedAt) {
}
