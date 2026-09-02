package com.moriba.skultem.application.dto;

import java.time.LocalDate;

// One row in the attendance history list - a past day's aggregate counts, without the full roster
// (see TeacherAttendanceRosterDTO for the per-teacher drill-down).
public record TeacherAttendanceDaySummaryDTO(
        LocalDate date,
        long presentCount,
        long lateCount,
        long absentCount,
        long excusedCount,
        long totalCount,
        double rate) {
}
