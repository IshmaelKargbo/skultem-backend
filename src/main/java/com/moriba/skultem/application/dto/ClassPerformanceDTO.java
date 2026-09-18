package com.moriba.skultem.application.dto;

// One row of the Academic Report's Class Performance comparison table (whole-school mode only).
// averageScore/passRate are derived from each assessed student's overall average, the same figure
// StudentAcademicPerformanceDTO.overallAverage reports for that class, so this table and a
// class-scoped report on the same class can never disagree.
public record ClassPerformanceDTO(
        String classId,
        String className,
        int totalStudents,
        int studentsAssessed,
        int studentsNotAssessed,
        double averageScore,
        double passRate) {
}
