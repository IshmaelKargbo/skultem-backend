package com.moriba.skultem.application.dto;

// One student's attendance performance over a date range (a month or a term) - backs Monthly
// Summary, Term Summary and their matching Inspection Report types. "schoolDays" is this
// student's own count of recorded (non-holiday) attendance days in the range, not the class's -
// a student enrolled mid-period is judged against their own recorded days, not the full period.
public record StudentAttendanceSummaryDTO(
        String studentId,
        String enrollmentId,
        String studentName,
        String admissionNumber,
        String className,
        String gender,
        long schoolDays,
        long present,
        long absent,
        long late,
        Double attendancePercentage,
        boolean belowThreshold) {
}
