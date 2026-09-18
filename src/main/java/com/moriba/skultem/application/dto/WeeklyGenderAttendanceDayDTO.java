package com.moriba.skultem.application.dto;

import java.time.LocalDate;

// One day's boys/girls attendance breakdown for a class - absent counts are enrolled-minus-present
// (not "total attendance records minus present"), so a day nobody marked attendance for a given
// student still reports a correct absent count rather than silently under-counting.
public record WeeklyGenderAttendanceDayDTO(
        LocalDate date,
        String dayLabel,
        int boysPresent,
        int boysAbsent,
        int girlsPresent,
        int girlsAbsent,
        double boysAttendancePercentage,
        double girlsAttendancePercentage,
        double overallAttendancePercentage) {
}
