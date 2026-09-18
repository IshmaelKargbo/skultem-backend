package com.moriba.skultem.application.dto;

import java.util.List;

// One row of the Academic Report's Subject Performance section. All figures are derived from
// each assessed student's average score IN THIS SUBJECT (approved cycles only) - see
// AssessmentScoreRepository#studentSubjectAveragesForReport. medianScore/highestScore/lowestScore
// are the median/max/min of those per-student subject averages, not raw individual assessment
// scores. gradeDistribution uses the school's own configured grading scale (School.gradingScale) -
// left empty if the school has no grading scale configured, rather than inventing one.
public record SubjectPerformanceDTO(
        String subjectId,
        String subjectName,
        double averageScore,
        double medianScore,
        double highestScore,
        double lowestScore,
        double passRate,
        int studentsAssessed,
        long passCount,
        long failCount,
        List<GradeCountDTO> gradeDistribution) {
}
