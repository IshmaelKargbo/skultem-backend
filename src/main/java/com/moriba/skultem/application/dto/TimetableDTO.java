package com.moriba.skultem.application.dto;

import java.time.Instant;

public record TimetableDTO(String id, String subject, String teacher, Instant createdAt, Instant updatedAt) {
}
