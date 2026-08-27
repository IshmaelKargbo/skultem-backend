package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.SchemeOfWork.State;
import com.moriba.skultem.domain.model.Week;

public record SchemeOfWorkDTO(String id, String subject, String subjectId, String term, String termId,
                String session, String sessionId, long weeks, LocalDate startDate, LocalDate endDate, State state,
                Week.State progressState, Instant createdAt, Instant updatedAt) {
}
