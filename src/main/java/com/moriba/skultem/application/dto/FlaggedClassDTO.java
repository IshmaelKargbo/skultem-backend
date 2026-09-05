package com.moriba.skultem.application.dto;

// One row in ClassAttentionSummaryDTO's list - classSessionId links straight to /classes/{clazzId}
// on the frontend (a class session's own id, not a class-subject or enrollment id).
public record FlaggedClassDTO(
        String classId,
        String className,
        String sectionName,
        String streamName,
        int flaggedCount,
        int totalStudents) {
}
