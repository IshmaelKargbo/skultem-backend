package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Holiday;
import com.moriba.skultem.domain.model.Period;
import com.moriba.skultem.domain.model.Room;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.model.Timetable;
import com.moriba.skultem.domain.model.WorkingDay;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.HolidayRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.repository.TimetableRepository;

// B-1 to B-6: sessions come from the school's own timetable - its Library periods, on their weekday, inside a
// term, never on a holiday.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LibrarySessionScheduleTest {

    // Mon 2 Nov 2026 .. Sun 15 Nov 2026
    private static final LocalDate MON = LocalDate.of(2026, 11, 2);

    @Mock
    private AcademicYearRepository academicYearRepo;
    @Mock
    private TermRepository termRepo;
    @Mock
    private HolidayRepository holidayRepo;
    @Mock
    private TimetableRepository timetableRepo;
    @InjectMocks
    private LibrarySessionSchedule schedule;

    @BeforeEach
    void setUp() {
        var year = mock(AcademicYear.class);
        when(year.getId()).thenReturn("year");
        when(academicYearRepo.findActiveBySchool("school")).thenReturn(Optional.of(year));
        var term = mock(Term.class);
        when(term.getId()).thenReturn("term-1");
        when(term.getStartDate()).thenReturn(MON);
        when(term.getEndDate()).thenReturn(MON.plusDays(11)); // Fri 13 Nov
        when(termRepo.findByAcademicYearIdAndSchool("year", "school")).thenReturn(List.of(term));
        when(holidayRepo.findAllBySchoolIdAndAcademicYear(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
    }

    private Timetable entry(String id, WorkingDay.Day weekday, boolean dayOn, Period.Type type) {
        var session = mock(ClassSession.class);
        when(session.getId()).thenReturn("cs-1");
        when(session.getName()).thenReturn("JSS 2 A");
        var period = mock(Period.class);
        when(period.getType()).thenReturn(type);
        when(period.getSession()).thenReturn(session);
        when(period.getName()).thenReturn("Period 3");
        when(period.getStartTime()).thenReturn(LocalTime.of(10, 0));
        when(period.getEndTime()).thenReturn(LocalTime.of(10, 40));
        var day = mock(WorkingDay.class);
        when(day.getDay()).thenReturn(weekday);
        when(day.isState()).thenReturn(dayOn);
        var room = mock(Room.class);
        when(room.getName()).thenReturn("Library Room");
        var entry = mock(Timetable.class);
        when(entry.getId()).thenReturn(id);
        when(entry.getPeriod()).thenReturn(period);
        when(entry.getDay()).thenReturn(day);
        when(entry.label()).thenReturn("Library");
        when(entry.isLibrary()).thenReturn(true);
        when(entry.getRoom()).thenReturn(room);
        return entry;
    }

    private void timetable(Timetable... entries) {
        when(timetableRepo.findAllLibraryByAcademicYear("year", "school")).thenReturn(List.of(entries));
    }

    @Test
    void aWeeklyLibraryPeriodGivesOneSessionPerWeekInsideTheTerm() {
        timetable(entry("e1", WorkingDay.Day.TUESDAY, true, Period.Type.PERIOD));

        var slots = schedule.slotsBetween(schedule.load("school").orElseThrow(), MON, MON.plusDays(20));

        // Tue 3 and Tue 10 Nov; Tue 17 Nov is after the term.
        assertThat(slots).extracting(s -> s.date()).containsExactly(MON.plusDays(1), MON.plusDays(8));
        assertThat(slots.get(0).className()).isEqualTo("JSS 2 A");
        assertThat(slots.get(0).roomName()).isEqualTo("Library Room");
        assertThat(slots.get(0).subjectName()).isEqualTo("Library");
        assertThat(slots.get(0).termEndDate()).isEqualTo(MON.plusDays(11));
    }

    @Test
    void holidaysAndSwitchedOffDaysHaveNoSession() {
        timetable(entry("e1", WorkingDay.Day.TUESDAY, true, Period.Type.PERIOD),
                entry("e2", WorkingDay.Day.WEDNESDAY, false, Period.Type.PERIOD));
        var holiday = mock(Holiday.class);
        when(holiday.getDate()).thenReturn(MON.plusDays(1));
        when(holidayRepo.findAllBySchoolIdAndAcademicYear(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(holiday)));

        var slots = schedule.slotsBetween(schedule.load("school").orElseThrow(), MON, MON.plusDays(6));

        assertThat(slots).isEmpty();
    }

    @Test
    void breaksAreNeverLibrarySessions() {
        timetable(entry("e1", WorkingDay.Day.TUESDAY, true, Period.Type.BREAK));

        assertThat(schedule.load("school").orElseThrow().entries()).isEmpty();
    }

    @Test
    void noLibraryPeriodsOnTheTimetableMeansNoSessions() {
        timetable();

        assertThat(schedule.slotsBetween(schedule.load("school").orElseThrow(), MON, MON.plusDays(6))).isEmpty();
        assertThatThrownBy(() -> schedule.slotFor("school", "e1", MON)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void noActiveAcademicYearMeansNoSessions() {
        when(academicYearRepo.findActiveBySchool("school")).thenReturn(Optional.empty());

        assertThat(schedule.load("school")).isEmpty();
        assertThatThrownBy(() -> schedule.slotFor("school", "e1", MON)).isInstanceOf(RuleException.class);
    }

    @Test
    void aDateWithoutASessionIsRefused() {
        timetable(entry("e1", WorkingDay.Day.TUESDAY, true, Period.Type.PERIOD));

        assertThat(schedule.slotFor("school", "e1", MON.plusDays(1)).date()).isEqualTo(MON.plusDays(1));
        assertThatThrownBy(() -> schedule.slotFor("school", "e1", MON)).isInstanceOf(RuleException.class);
    }
}
