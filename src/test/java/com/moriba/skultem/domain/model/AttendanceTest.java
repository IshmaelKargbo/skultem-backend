package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

// Regression coverage for the parent portal showing a late arrival as 100% present: getStatus()
// used to check present before late, and a late mark is recorded as present=true, late=true (the
// student did show up, just late) - so it always won and "Late" was dead code. Every attendance
// percentage across the app (parent calendar, dashboard, breakdown, report, teacher dashboard,
// generic widgets) is derived from this one method via AttendanceMapper, so this single ordering
// bug fanned out everywhere.
class AttendanceTest {

    private Attendance attendanceWith(boolean present, boolean excused, boolean late, boolean holiday) {
        return Attendance.create("A-1", "school-1", null, LocalDate.now(), present, excused, late, null, holiday,
                null);
    }

    @Test
    void aPresentAndLateMarkReportsAsLateRatherThanPresent() {
        assertThat(attendanceWith(true, false, true, false).getStatus()).isEqualTo("Late");
    }

    @Test
    void aPlainPresentMarkStillReportsAsPresent() {
        assertThat(attendanceWith(true, false, false, false).getStatus()).isEqualTo("Present");
    }

    @Test
    void anExcusedAbsenceReportsAsExcused() {
        assertThat(attendanceWith(false, true, false, false).getStatus()).isEqualTo("Excused");
    }

    @Test
    void aHolidayReportsAsHolidayWhenNotPresent() {
        assertThat(attendanceWith(false, false, false, true).getStatus()).isEqualTo("Holiday");
    }

    @Test
    void anUnmarkedDayReportsAsAbsent() {
        assertThat(attendanceWith(false, false, false, false).getStatus()).isEqualTo("Absent");
    }
}
