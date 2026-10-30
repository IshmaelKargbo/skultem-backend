package com.moriba.skultem.application.dto;

import java.time.Instant;

// kind: SUBJECT or LIBRARY. A Library period has subject "Library" and no subjectId or teacher.
public record TimetableDTO(String id, String subject, String subjectId, String teacher, String room, String roomId, String color, Instant createdAt, Instant updatedAt, String kind) {
}
