package com.moriba.skultem.application.dto;

import java.time.Instant;

public record TimetableDTO(String id, String subject, String subjectId, String teacher, String room, String roomId, String color, Instant createdAt, Instant updatedAt) {
}
