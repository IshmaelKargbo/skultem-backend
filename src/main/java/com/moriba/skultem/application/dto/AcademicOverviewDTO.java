package com.moriba.skultem.application.dto;

// Summary cards for the Academic Report. totalAssessments/completedAssessments count assessment
// CYCLES (one per subject x assessment component scheduled for the class/term), not per-student
// scores. classAverage/passRate/studentsNeedingSupport are derived from each student's overall
// average (the same figure StudentAcademicPerformanceDTO.overallAverage reports), so this card and
// the Student Performance table can never disagree.
public record AcademicOverviewDTO(
        int totalStudents,
        int studentsAssessed,
        int totalAssessments,
        int completedAssessments,
        double classAverage,
        double passRate,
        int studentsNeedingSupport) {
}
