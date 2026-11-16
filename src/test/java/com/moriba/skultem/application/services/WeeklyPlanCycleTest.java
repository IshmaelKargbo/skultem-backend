package com.moriba.skultem.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

// A repeating plan: the term's first week is week 1 of the pattern, and it starts over after cycleWeeks weeks.
class WeeklyPlanCycleTest {

    private static final LocalDate FIRST_MONDAY = LocalDate.of(2026, 9, 7);

    @Test
    void firstWeekOfTermIsWeekOne() {
        assertEquals(1, WeeklyPlanService.cycleWeek(FIRST_MONDAY, FIRST_MONDAY, 4));
        assertEquals(1, WeeklyPlanService.cycleWeek(FIRST_MONDAY, LocalDate.of(2026, 9, 11), 4));
    }

    @Test
    void fourWeekMonthStartsOverOnWeekFive() {
        assertEquals(2, WeeklyPlanService.cycleWeek(FIRST_MONDAY, LocalDate.of(2026, 9, 14), 4));
        assertEquals(4, WeeklyPlanService.cycleWeek(FIRST_MONDAY, LocalDate.of(2026, 10, 2), 4));
        assertEquals(1, WeeklyPlanService.cycleWeek(FIRST_MONDAY, LocalDate.of(2026, 10, 5), 4));
        assertEquals(3, WeeklyPlanService.cycleWeek(FIRST_MONDAY, LocalDate.of(2026, 10, 21), 4));
    }

    @Test
    void sameEveryWeek() {
        assertEquals(1, WeeklyPlanService.cycleWeek(FIRST_MONDAY, LocalDate.of(2026, 11, 18), 1));
    }
}
