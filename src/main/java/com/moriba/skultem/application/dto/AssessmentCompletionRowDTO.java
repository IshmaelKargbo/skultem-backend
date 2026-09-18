package com.moriba.skultem.application.dto;

import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;

// One row of the Assessment Completion Report - a real ClassSubjectAssessmentLifeCycle's status,
// never an invented "expected assessment count". A class taught in multiple sessions (streams/
// sections) can produce more than one row for the same (class, subject, assessment) - each is a
// distinct teaching group's own cycle, not a duplicate.
public record AssessmentCompletionRowDTO(
        String classId,
        String className,
        String subjectId,
        String subjectName,
        String assessmentId,
        String assessmentName,
        ClassSubjectAssessmentLifeCycle.Status status) {
}
