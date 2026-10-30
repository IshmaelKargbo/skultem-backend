package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Period;
import com.moriba.skultem.domain.model.Room;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.model.Timetable;
import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.domain.model.WorkingDay;
import com.moriba.skultem.domain.model.WorkingDay.Day;
import com.moriba.skultem.domain.repository.PeriodRepository;
import com.moriba.skultem.domain.repository.RoomRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.domain.repository.TimetableRepository;
import com.moriba.skultem.domain.repository.WorkingDayRepository;

// A school sets Library periods on the timetable like lessons. A Library period has no subject or teacher and
// takes the whole period for the class, so it never sits beside subject options.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreateTimetableUseCaseTest {

    @Mock
    private TeacherSubjectRepository teacherSubjectRepo;
    @Mock
    private RoomRepository roomRepo;
    @Mock
    private PeriodRepository periodRepo;
    @Mock
    private WorkingDayRepository dayRepo;
    @Mock
    private TimetableRepository repo;
    @Mock
    private ResolveTimingForLevelUseCase resolveTimingForLevelUseCase;
    @Mock
    private LogActivityUseCase logActivityUseCase;
    @InjectMocks
    private CreateTimetableUseCase useCase;

    private Period period;
    private WorkingDay day;
    private Room room;

    @BeforeEach
    void setUp() {
        var session = mock(ClassSession.class);
        when(session.getName()).thenReturn("JSS 1 A");
        period = mock(Period.class);
        when(period.getSession()).thenReturn(session);
        when(periodRepo.findById("p1")).thenReturn(Optional.of(period));
        var timing = mock(Timing.class);
        when(timing.getId()).thenReturn("tm");
        when(resolveTimingForLevelUseCase.execute(anyString(), any())).thenReturn(timing);
        day = mock(WorkingDay.class);
        when(day.getId()).thenReturn("wd");
        when(dayRepo.findByDayAndTimingId(Day.TUESDAY, "tm")).thenReturn(Optional.of(day));
        room = mock(Room.class);
        when(roomRepo.findByIdAndSchoolId("r1", "school")).thenReturn(Optional.of(room));
        when(repo.findAllByPeriodIdAndDayIdAndSchoolId("p1", "wd", "school")).thenReturn(List.of());
    }

    private Timetable mathsLesson() {
        var subject = mock(Subject.class);
        when(subject.getId()).thenReturn("maths");
        when(subject.getName()).thenReturn("Mathematics");
        var teacherSubject = mock(TeacherSubject.class);
        when(teacherSubject.getSubject()).thenReturn(subject);
        return Timetable.create("t-maths", "school", period, teacherSubject, day, room, "blue");
    }

    @Test
    void aLibraryPeriodNeedsNoSubjectOrTeacher() {
        var saved = useCase.execut("school", "p1", null, Day.TUESDAY, "r1", "blue", null, Timetable.Kind.LIBRARY);

        assertThat(saved.isLibrary()).isTrue();
        assertThat(saved.getTeacherSubject()).isNull();
        assertThat(saved.label()).isEqualTo("Library");
        verify(repo).save(saved);
    }

    @Test
    void aLessonStillNeedsASubject() {
        assertThatThrownBy(() -> useCase.execut("school", "p1", " ", Day.TUESDAY, "r1", "blue", null,
                Timetable.Kind.SUBJECT)).isInstanceOf(RuleException.class).hasMessageContaining("Subject");
        verify(repo, never()).save(any());
    }

    @Test
    void aLibraryPeriodCantSitBesideALesson() {
        var lesson = mathsLesson();
        when(repo.findAllByPeriodIdAndDayIdAndSchoolId("p1", "wd", "school")).thenReturn(List.of(lesson));

        assertThatThrownBy(() -> useCase.execut("school", "p1", null, Day.TUESDAY, "r1", "blue", null,
                Timetable.Kind.LIBRARY)).isInstanceOf(RuleException.class).hasMessageContaining("Mathematics");
        verify(repo, never()).save(any());
    }

    @Test
    void aLessonCantBeAddedToALibraryPeriod() {
        var library = Timetable.createLibrary("t-lib", "school", period, day, room, "blue");
        when(repo.findAllByPeriodIdAndDayIdAndSchoolId("p1", "wd", "school")).thenReturn(List.of(library));
        var ts = mock(TeacherSubject.class);
        when(ts.getSubject()).thenReturn(mock(Subject.class));
        when(teacherSubjectRepo.findById("ts")).thenReturn(Optional.of(ts));

        assertThatThrownBy(() -> useCase.execut("school", "p1", "ts", Day.TUESDAY, "r1", "blue", null,
                Timetable.Kind.SUBJECT)).isInstanceOf(RuleException.class).hasMessageContaining("Library period");
    }

    @Test
    void aLessonCanBeTurnedIntoALibraryPeriod() {
        var lesson = mathsLesson();
        when(repo.findAllByPeriodIdAndDayIdAndSchoolId("p1", "wd", "school")).thenReturn(List.of(lesson));

        useCase.execut("school", "p1", null, Day.TUESDAY, "r1", "green", "t-maths", Timetable.Kind.LIBRARY);

        var saved = ArgumentCaptor.forClass(Timetable.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo("t-maths");
        assertThat(saved.getValue().isLibrary()).isTrue();
        assertThat(saved.getValue().getTeacherSubject()).isNull();
    }

    @Test
    void breaksCantBeLibraryPeriods() {
        when(period.isBreak()).thenReturn(true);

        assertThatThrownBy(() -> useCase.execut("school", "p1", null, Day.TUESDAY, "r1", "blue", null,
                Timetable.Kind.LIBRARY)).isInstanceOf(RuleException.class);
    }
}
