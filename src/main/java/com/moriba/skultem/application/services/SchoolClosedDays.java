package com.moriba.skultem.application.services;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.repository.CalendarEventRepository;
import com.moriba.skultem.domain.repository.HolidayRepository;

import lombok.RequiredArgsConstructor;

// The days the school is closed: its holidays, plus every day of a whole-school calendar entry marked
// "no school". Weekends aren't included - SchoolDays handles those. Every school-day count goes through
// this so attendance figures, marking and library sessions all agree on which days are school days.
@Service
@RequiredArgsConstructor
public class SchoolClosedDays {

    // A runaway entry (a typo'd end year) must not blow up into thousands of dates.
    private static final int MAX_ENTRY_DAYS = 366;

    private final HolidayRepository holidayRepo;
    private final CalendarEventRepository calendarRepo;

    // Only the [.., date at dateIndex, ..] rows on a weekday the school was open - a mark on a weekend or a
    // closed day isn't a school day and shouldn't sway anyone's attendance.
    public static java.util.List<Object[]> onSchoolDays(java.util.List<Object[]> rows, int dateIndex,
            Set<LocalDate> closed) {
        return rows.stream().filter(r -> {
            var d = (LocalDate) r[dateIndex];
            var dow = d.getDayOfWeek();
            return dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY && !closed.contains(d);
        }).toList();
    }

    public Set<LocalDate> forYear(String schoolId, String academicYearId) {
        Set<LocalDate> closed = new HashSet<>();
        holidayRepo.findAllBySchoolIdAndAcademicYear(schoolId, academicYearId, Pageable.unpaged())
                .getContent().forEach(h -> closed.add(h.getDate()));

        for (var entry : calendarRepo.findNoSchoolDays(schoolId)) {
            if (entry.getStartDate() == null) continue;
            // Entries are whole days stored as UTC midnight of the picked day.
            LocalDate from = entry.getStartDate().atZone(ZoneOffset.UTC).toLocalDate();
            LocalDate to = entry.getEndDate() == null ? from : entry.getEndDate().atZone(ZoneOffset.UTC).toLocalDate();
            int n = 0;
            for (LocalDate d = from; !d.isAfter(to) && n < MAX_ENTRY_DAYS; d = d.plusDays(1), n++) {
                closed.add(d);
            }
        }
        return closed;
    }
}
