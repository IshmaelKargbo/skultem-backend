package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import com.moriba.skultem.application.services.SchoolClosedDays;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.CalendarEvent;
import com.moriba.skultem.domain.model.Holiday;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.CalendarEventRepository;
import com.moriba.skultem.domain.repository.HolidayRepository;
import com.moriba.skultem.domain.repository.TermRepository;

class ResolveTermSchoolDaysUseCaseTest {

    private final ResolveAcademicYearUseCase years = mock(ResolveAcademicYearUseCase.class);
    private final TermRepository terms = mock(TermRepository.class);
    private final HolidayRepository holidays = mock(HolidayRepository.class);
    private final CalendarEventRepository calendar = mock(CalendarEventRepository.class);
    private final ResolveTermSchoolDaysUseCase useCase = new ResolveTermSchoolDaysUseCase(years, terms, mock(com.moriba.skultem.domain.repository.AcademicYearRepository.class),
            new SchoolClosedDays(holidays, calendar));

    @Test
    void everyWeekdayFromTheTermStartLessHolidaysAndNoSchoolDays() {
        var year = mock(AcademicYear.class);
        when(year.getId()).thenReturn("y");
        when(years.execute(anyString(), any())).thenReturn(year);
        var t = mock(Term.class);
        // Wed 2 Sep to Fri 11 Sep 2026: 8 weekdays.
        when(t.getStartDate()).thenReturn(LocalDate.of(2026, 9, 2));
        when(t.getEndDate()).thenReturn(LocalDate.of(2026, 9, 11));
        when(terms.findActiveBySchoolAndAcademicYear("s", "y")).thenReturn(Optional.of(t));
        var holiday = mock(Holiday.class);
        when(holiday.getDate()).thenReturn(LocalDate.of(2026, 9, 8));
        when(holidays.findAllBySchoolIdAndAcademicYear(anyString(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of(holiday)));
        // A "no school" event over Thu 10 - Fri 11 Sep.
        var closed = CalendarEvent.create("e", "s", "Sports break", "x", CalendarEvent.Type.EVENT,
                Instant.parse("2026-09-10T00:00:00Z"), Instant.parse("2026-09-11T00:00:00Z"), null, "u", "U", null,
                true);
        when(calendar.findNoSchoolDays("s")).thenReturn(List.of(closed));

        var days = useCase.execute("s", null, null).schoolDays();
        assertThat(days).hasSize(5).doesNotContain(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 5));
    }

    @Test
    void aSectionEntryCannotCloseTheSchool() {
        var entry = CalendarEvent.create("e", "s", "Trip", "x", CalendarEvent.Type.HOLIDAY,
                Instant.parse("2026-09-10T00:00:00Z"), Instant.parse("2026-09-10T00:00:00Z"), null, "u", "U",
                "section-1", true);
        assertThat(entry.isNoSchool()).isFalse();
    }
}
