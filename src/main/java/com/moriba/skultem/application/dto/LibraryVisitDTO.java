package com.moriba.skultem.application.dto;

import java.time.Instant;

// status is OPEN, CHECKED_OUT or RESOLVED; unresolved = still OPEN after the student's school day ended.
// durationMinutes is only set for a visit closed by a real check-out.
public record LibraryVisitDTO(String id, String studentId, String studentName, String studentClass, String status,
        boolean unresolved, Instant checkedInAt, String checkedInByName, Instant schoolDayEndsAt,
        Instant checkedOutAt, String checkedOutByName, Long durationMinutes, Instant resolvedAt,
        String resolvedByName, String resolutionReason) {
}
