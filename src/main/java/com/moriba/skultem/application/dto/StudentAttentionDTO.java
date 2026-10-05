package com.moriba.skultem.application.dto;

import com.moriba.skultem.domain.service.AttendanceAttentionCalculator.Level;
import com.moriba.skultem.domain.service.AttendanceAttentionCalculator.Trend;

// One flagged student in ComputeClassAttentionUseCase's result. Rates/averages are null when there
// isn't enough data yet (fewer than a handful of recorded days, or too few scores this term) rather
// than 0, so the UI can tell "no data" apart from "actually zero".
//   attendanceRate     - the recent window (last ~20 recorded school days), what drives the flag
//   termAttendanceRate - term to date; tells a chronic problem from a bad fortnight
//   attendanceTrend    - recent vs the 20 days before it (null when there's no earlier window)
//   previousTermAverage / academicTrend - the term before, for improving vs declining
//   severity           - the worst of the attendance level and the academic flag
public record StudentAttentionDTO(
        String studentId,
        String enrollmentId,
        String givenNames,
        String familyName,
        String photo,
        Double attendanceRate,
        Double academicAverage,
        boolean attendanceFlag,
        boolean academicFlag,
        Double termAttendanceRate,
        Trend attendanceTrend,
        int absenceStreak,
        Level severity,
        Double previousTermAverage,
        Trend academicTrend) {
}
