package com.moriba.skultem.application.dto;

import java.time.LocalDate;
import java.util.List;

// Result of GenerateWeeklyGenderAttendanceReportUseCase. boysEnrolled/girlsEnrolled come from the
// current class roster (EnrollmentRepository), not from attendance records - see
// AttendanceRepository#attendanceCountsByClassGenderAndDateRange.
public record WeeklyGenderAttendanceDTO(
        String classId,
        String className,
        LocalDate weekStart,
        LocalDate weekEnd,
        int boysEnrolled,
        int girlsEnrolled,
        List<WeeklyGenderAttendanceDayDTO> days,
        double overallAttendancePercentage,
        double boysAttendancePercentage,
        double girlsAttendancePercentage) {
}
