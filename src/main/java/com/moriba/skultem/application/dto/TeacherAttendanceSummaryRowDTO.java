package com.moriba.skultem.application.dto;

// One teacher's attendance performance over a date range (a month or a term) - backs Monthly
// Summary, Term Summary and Management Reports. workingDays is this teacher's own count of days
// they have a recorded row in the range (present, late, absent or excused) - not a calendar count,
// and not shared across teachers, so a teacher who joined mid-period isn't penalized for days
// before they existed. No belowThreshold flag - unlike student attendance, a threshold is not
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
