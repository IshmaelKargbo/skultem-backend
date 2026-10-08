package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

// One day's attendance for one section/stream of a class (SSS 1 Art, not all of SSS 1). The JPA query fills
// everything but sessionId (selected as an empty placeholder), which the use case adds so the page can open that exact class session.
public record AttendanceHistoryDTO(
                LocalDate date,
                String classId,
                String className,
                Long presentCount,
                Long totalCount,
                Instant createdAt,
                Instant updatedAt,
                String sectionName,
                String streamName,
                String sessionId
                ) {

        public AttendanceHistoryDTO withSessionId(String sessionId) {
                return new AttendanceHistoryDTO(date, classId, className, presentCount, totalCount, createdAt,
                                updatedAt, sectionName, streamName, sessionId);
        }
}
