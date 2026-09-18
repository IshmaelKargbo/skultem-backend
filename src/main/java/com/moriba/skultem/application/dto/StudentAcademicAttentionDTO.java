package com.moriba.skultem.application.dto;

import com.moriba.skultem.domain.service.PerformanceTrendCalculator.Trend;

// One student in the Students Requiring Attention section. Deliberately exposes each signal
// separately (lowAcademic/lowAttendance/missingAssessments/decliningTrend) rather than folding
// them into one combined score, per the requirement to give management evidence rather than an
// unsupported diagnosis - a caller decides what "needs attention" means by reading the flags, not
// by trusting a single number this DTO doesn't produce. academicAverage/attendanceRate are null
// when there isn't enough data yet (no scores this term / no attendance recorded), same convention
// StudentAttentionDTO already uses.
public record StudentAcademicAttentionDTO(
        String studentId,
        String enrollmentId,
        String givenNames,
        String familyName,
        String photo,
        Double academicAverage,
        Double attendanceRate,
        long assessmentsMissing,
        Trend trend,
        boolean lowAcademicSignal,
        boolean lowAttendanceSignal,
        boolean missingAssessmentsSignal,
        boolean decliningTrendSignal) {
}
