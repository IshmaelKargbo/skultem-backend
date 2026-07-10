package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.SchemeOfWork.State;

public record SchemeOfWorkDTO(String id, String subject, String subjectId, String term, String termId,
                String session, String sessionId, long weeks, LocalDate startDate, LocalDate endDate, State state, Instant createdAt, Instant updatedAt) {
}
