package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SkullyRecentAttendanceTest {

    @Test
    void aWeekCountsOnlySchoolDaysAndAnUnmarkedDayIsNotAnAbsence() {
        LocalDate mon = LocalDate.of(2026, 11, 23);
        // Wednesday was a no-school day, so it isn't a school day at all.
        Set<LocalDate> open = Set.of(mon, mon.plusDays(1), mon.plusDays(3), mon.plusDays(4));
        Map<LocalDate, String> marks = Map.of(mon, "PRESENT", mon.plusDays(1), "LATE", mon.plusDays(3), "ABSENT",
                mon.plusDays(2), "PRESENT");

        var week = SkullyRecentAttendance.week(mon, mon.plusDays(4), open, marks);

        assertThat(week.schoolDays()).isEqualTo(4);
        assertThat(week.present()).isEqualTo(1);
        assertThat(week.late()).isEqualTo(1);
        assertThat(week.absent()).isEqualTo(1);
        assertThat(week.notMarked()).isEqualTo(1); // Friday: nobody took the register
        assertThat(week.days()).extracting(SkullyRecentAttendance.Day::day)
                .containsExactly("Monday", "Tuesday", "Thursday", "Friday");
        assertThat(week.days().get(3).status()).isEqualTo("NOT_MARKED");
    }
}
