package com.moriba.skultem.application.dto;

// Summary cards for the Academic Report. totalAssessments/completedAssessments count assessment
// CYCLES (one per subject x assessment component scheduled for the class/term), not per-student
// scores. classAverage/passRate/studentsNeedingSupport are derived from each student's overall
// average (the same figure StudentAcademicPerformanceDTO.overallAverage reports), so this card and
// the Student Performance table can never disagree.
//
// passMark is the pass mark those figures were measured against (the class's assessment template pass
// mark) so the card can say so. It's a single number only when every class in the report shares it; when
// classes in a whole-school report use different templates there is no one pass mark to show, so it's null.
public record AcademicOverviewDTO(
        int totalStudents,
        int studentsAssessed,
        int totalAssessments,
        int completedAssessments,
        double classAverage,
        double passRate,
        int studentsNeedingSupport,
        Integer passMark) {
}
