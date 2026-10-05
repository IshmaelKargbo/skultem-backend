package com.moriba.skultem.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.service.AttendanceAttentionCalculator.Day;
import com.moriba.skultem.domain.service.AttendanceAttentionCalculator.Level;
import com.moriba.skultem.domain.service.AttendanceAttentionCalculator.Trend;

class AttendanceAttentionCalculatorTest {

    private static final double THRESHOLD = 75.0;
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 2);

    // pattern is oldest -> newest, 'P' present, 'A' absent; one day per index, ending today.
    private static List<Day> days(String pattern) {
        List<Day> out = new ArrayList<>();
        for (int i = 0; i < pattern.length(); i++) {
            out.add(new Day(TODAY.minusDays(pattern.length() - 1 - i), pattern.charAt(i) == 'P'));
        }
        return out;
    }

    private static String repeat(String s, int n) {
        return s.repeat(n);
    }

    @Test
    void notEnoughDataIsNeverFlaggedOnRate() {
        var r = AttendanceAttentionCalculator.evaluate(days("PAPA"), THRESHOLD, null);
        assertNull(r.recentRate());
        assertEquals(Level.NONE, r.level());
    }

    @Test
    void goodAttendanceIsNotFlagged() {
        var r = AttendanceAttentionCalculator.evaluate(days(repeat("P", 20)), THRESHOLD, null);
        assertEquals(100.0, r.recentRate());
        assertEquals(Level.NONE, r.level());
    }

    @Test
    void threeAbsencesInARowIsCriticalEvenWithFewDays() {
        var r = AttendanceAttentionCalculator.evaluate(days("PPAAA"), THRESHOLD, null);
        assertEquals(3, r.absenceStreak());
        assertEquals(Level.CRITICAL, r.level());
    }

    @Test
    void recentDipButGoodTermIsOnlyWatch() {
        // 20 perfect days, then two steady 70% windows: recent 70% (below 75) but steady, term 80%.
        String steady = repeat("AAAPPPPPPP", 2);
        var r = AttendanceAttentionCalculator.evaluate(days(repeat("P", 20) + steady + steady), THRESHOLD, null);
        assertEquals(Trend.STEADY, r.trend());
        assertEquals(Level.WATCH, r.level());
    }

    @Test
    void lowRecentAndLowTermSteadyNeedsAttention() {
        // 40 days at 60% throughout.
        String block = "PAPAP"; // 60% per 5 days
        var r = AttendanceAttentionCalculator.evaluate(days(repeat(block, 8)), THRESHOLD, null);
        assertEquals(Trend.STEADY, r.trend());
        assertEquals(Level.NEEDS_ATTENTION, r.level());
    }

    @Test
    void decliningBelowThresholdIsCritical() {
        // Previous 20 days perfect, recent 20 days 50%.
        var r = AttendanceAttentionCalculator.evaluate(days(repeat("P", 20) + repeat("PA", 10)), THRESHOLD, null);
        assertEquals(Trend.DECLINING, r.trend());
        assertEquals(Level.CRITICAL, r.level());
    }

    @Test
    void recoveringChildDropsToWatchThenClears() {
        // Previous 20 days at 40%, recent 20 days at 70%: still below 75 but improving, term below too.
        String prev = repeat("PAPAA", 4);   // 40%
        String recent = repeat("PPAPPPAPPA", 2); // 70%
        var improving = AttendanceAttentionCalculator.evaluate(days(prev + recent), THRESHOLD, null);
        assertEquals(Trend.IMPROVING, improving.trend());
        assertEquals(Level.WATCH, improving.level());

        // Recent window back above the bar -> no flag at all, regardless of the bad history.
        String better = repeat("PPPPPPPPPA", 2); // 90%
        var recovered = AttendanceAttentionCalculator.evaluate(days(prev + better), THRESHOLD, null);
        assertEquals(Level.NONE, recovered.level());
    }

    @Test
    void termToDateIgnoresDaysBeforeTermStart() {
        // Bad old term (all absent) then a clean new term: term-to-date should only see the new one.
        List<Day> all = new ArrayList<>(days(repeat("A", 10) + repeat("P", 10)));
        LocalDate termStart = TODAY.minusDays(9);
        var r = AttendanceAttentionCalculator.evaluate(all, THRESHOLD, termStart);
        assertEquals(100.0, r.termRate());
    }
}
