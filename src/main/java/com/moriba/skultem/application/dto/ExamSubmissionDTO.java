package com.moriba.skultem.application.dto;

import java.time.Instant;

// One teaching group's assessment for the term and where it stands - what the exam office chases.
// status: DRAFT (being entered), SUBMITTED (waiting for approval), RETURNED, APPROVED or COMPLETED.
public record ExamSubmissionDTO(String cycleId, String classSessionId, String classId, String className,
        String level, String subjectName, String assessmentName, int assessmentPosition, String teacherId,
        String teacherName, String status, Instant updatedAt) {
}
