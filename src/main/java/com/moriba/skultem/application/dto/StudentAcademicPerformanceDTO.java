package com.moriba.skultem.application.dto;

import java.util.List;

// One row of the Academic Report's Student Performance section. overallAverage is the average of
// the student's own subject averages (subjects), not a raw average across every score, so a
// subject with more assessment components doesn't outweigh one with fewer. assessmentsCompleted/
// Missing count individual assessment components (not subjects) - see
// AssessmentScoreRepository#assessmentCompletionByClassAndTerm.
public record StudentAcademicPerformanceDTO(
        String studentId,
        String enrollmentId,
        String givenNames,
        String familyName,
        List<StudentSubjectScoreDTO> subjects,
        double overallAverage,
        long assessmentsCompleted,
        long assessmentsMissing) {
}
