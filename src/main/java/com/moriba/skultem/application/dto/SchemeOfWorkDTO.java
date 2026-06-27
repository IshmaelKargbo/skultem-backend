package com.moriba.skultem.application.dto;

import java.time.Instant;

public record SchemeOfWorkDTO(String id, String subject, String subjectId, String term, String termId,
                String session, String sessionId, long weeks, Instant createdAt, Instant updatedAt) {
}
