package com.moriba.skultem.application.dto;

// One teacher's attendance performance over a date range (a month or a term) - backs Monthly
// Summary, Term Summary and Management Reports. workingDays is the school days in the range so far
// (weekdays in a term, less the days the school was closed), the same for every teacher; a day with no
// record counts as absent. No belowThreshold flag - unlike student attendance, a threshold is not
// applied to teachers automatically (management can review the numbers themselves).
public record TeacherAttendanceSummaryRowDTO(
        String teacherId,
        String teacherName,
        long workingDays,
        long present,
        long absent,
        long late,
        Double attendancePercentage) {
}
