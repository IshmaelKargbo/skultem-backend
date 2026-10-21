package com.moriba.skultem.domain.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

// The days attendance is expected to be marked: weekdays that aren't one of the school's holidays -
// the same rule MarkClassSessionAttendanceUseCase enforces when a day is marked. Used to measure
// attendance against every school day so far, not only the days someone happened to record.
public final class SchoolDays {

    private SchoolDays() {
    }

    // Every school day from start to end, both inclusive; empty when end is before start.
    public static List<LocalDate> between(LocalDate start, LocalDate end, Collection<LocalDate> holidays) {
        var closed = new HashSet<>(holidays);
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY && !closed.contains(d)) {
                days.add(d);
            }
        }
        return days;
    }
}
