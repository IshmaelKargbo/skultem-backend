package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
import org.springframework.dao.DataIntegrityViolationException;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.LibrarySession;
import com.moriba.skultem.domain.model.LibrarySessionRecord;
import com.moriba.skultem.domain.model.LibraryVisit;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRecordRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRepository;
import com.moriba.skultem.domain.repository.LibraryVisitRepository;
import com.moriba.skultem.domain.repository.StudentRepository;

// AR-1 / AR-5: one open visit per student, and a bulk check-in saves everyone or no one.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CheckInLibraryVisitorsUseCaseTest {

    // 09:00 in Freetown (UTC+0).
    private static final Instant NOW = Instant.parse("2026-10-29T09:00:00Z");

    @Mock
    private StudentRepository studentRepo;
    @Mock
    private AcademicYearRepository academicYearRepo;
    @Mock
    private EnrollmentRepository enrollmentRepo;
    @Mock
    private LibraryVisitRepository visitRepo;
    @Mock
    private LibrarySessionRepository sessionRepo;
    @Mock
    private LibrarySessionRecordRepository recordRepo;
    @Mock
    private ResolveTimingForLevelUseCase resolveTimingUseCase;
    @Mock
    private LibraryAttendanceSupport support;
    @Mock
    private LogActivityUseCase logActivityUseCase;
    @InjectMocks
    private CheckInLibraryVisitorsUseCase useCase;

    @BeforeEach
    void setUp() {
        var year = mock(AcademicYear.class);
        when(year.getId()).thenReturn("year-1");
        when(academicYearRepo.findActiveBySchool("school")).thenReturn(Optional.of(year));
        when(support.now()).thenReturn(NOW);
        when(support.userName("user")).thenReturn("Librarian");
        var timing = mock(Timing.class);
        when(timing.getEndTime()).thenReturn(LocalTime.of(14, 30));
        when(resolveTimingUseCase.execute(eq("school"), any())).thenReturn(timing);
        when(visitRepo.findOpenByStudents(eq("school"), any())).thenReturn(List.of());
    }

    private void enrolled(String id, Enrollment.Status status) {
        var student = mock(Student.class);
        when(student.getId()).thenReturn(id);
        when(student.getName()).thenReturn("Student " + id);
        when(student.getStatus()).thenReturn(Student.Status.ACTIVE);
        when(studentRepo.findByIdAndSchoolId(id, "school")).thenReturn(Optional.of(student));

        var clazz = mock(Clazz.class);
        when(clazz.getName()).thenReturn("JSS 2");
        var enrollment = mock(Enrollment.class);
        when(enrollment.getStatus()).thenReturn(status);
        when(enrollment.getClazz()).thenReturn(clazz);
        when(enrollmentRepo.findByStudentAndAcademicYearAndSchoolId(id, "year-1", "school"))
                .thenReturn(Optional.of(enrollment));
    }

    private LibraryVisit openVisit(String studentId) {
        return LibraryVisit.checkIn("school", studentId, "Student " + studentId, "JSS 2", "user", "Librarian",
                NOW.minusSeconds(600), NOW.plusSeconds(3600));
    }

    @Test
    void everyoneEligibleIsCheckedInWithTheEndOfTheirSchoolDay() {
        enrolled("a", Enrollment.Status.ACTIVE);
        enrolled("b", Enrollment.Status.ACTIVE);

        var res = useCase.execute("school", List.of("a", "b"), false, "user");

        assertThat(res.checkedIn()).hasSize(2);
        assertThat(res.refused()).isEmpty();
        var saved = ArgumentCaptor.forClass(LibraryVisit.class);
        verify(visitRepo, times(2)).save(saved.capture());
        assertThat(saved.getValue().getSchoolDayEndsAt()).isEqualTo(Instant.parse("2026-10-29T14:30:00Z"));
        assertThat(saved.getValue().getStudentClass()).isEqualTo("JSS 2");
        assertThat(saved.getValue().getCheckedInByName()).isEqualTo("Librarian");
    }

    @Test
    void oneStudentAlreadyInStopsTheWholeCheckIn() {
        enrolled("a", Enrollment.Status.ACTIVE);
        enrolled("b", Enrollment.Status.ACTIVE);
        when(visitRepo.findOpenByStudents(eq("school"), any())).thenReturn(List.of(openVisit("b")));

        var res = useCase.execute("school", List.of("a", "b"), false, "user");

        assertThat(res.checkedIn()).isEmpty();
        assertThat(res.refused()).singleElement().satisfies(r -> {
            assertThat(r.studentId()).isEqualTo("b");
            assertThat(r.reason()).isEqualTo("Is already checked in");
        });
        verify(visitRepo, never()).save(any());
    }

    @Test
    void onlyTheEligibleStudentsAreCheckedInWhenAskedFor() {
        enrolled("a", Enrollment.Status.ACTIVE);
        enrolled("b", Enrollment.Status.LEFT);

        var res = useCase.execute("school", List.of("a", "b"), true, "user");

        assertThat(res.checkedIn()).extracting(v -> v.studentId()).containsExactly("a");
        assertThat(res.refused()).extracting(r -> r.reason()).containsExactly("Is not currently enrolled");
        verify(visitRepo, times(1)).save(any());
    }

    @Test
    void aStudentFromAnotherSchoolIsRefused() {
        when(studentRepo.findByIdAndSchoolId("x", "school")).thenReturn(Optional.empty());

        var res = useCase.execute("school", List.of("x"), false, "user");

        assertThat(res.refused()).extracting(r -> r.reason()).containsExactly("Student not found");
        verify(visitRepo, never()).save(any());
    }

    @Test
    void aCheckInThatLosesTheRaceIsRefusedNotDuplicated() {
        enrolled("a", Enrollment.Status.ACTIVE);
        doThrow(new DataIntegrityViolationException("uq_library_visits_one_open")).when(visitRepo).save(any());

        assertThatThrownBy(() -> useCase.execute("school", List.of("a"), false, "user"))
                .isInstanceOf(RuleException.class)
                .hasMessageContaining("already checked in");
    }

    @Test
    void anAfterSchoolVisitHasUntilMidnight() {
        enrolled("a", Enrollment.Status.ACTIVE);
        when(support.now()).thenReturn(Instant.parse("2026-10-29T15:00:00Z"));

        useCase.execute("school", List.of("a"), false, "user");

        var saved = ArgumentCaptor.forClass(LibraryVisit.class);
        verify(visitRepo).save(saved.capture());
        assertThat(saved.getValue().getSchoolDayEndsAt()).isEqualTo(Instant.parse("2026-10-30T00:00:00Z"));
    }

    @Test
    void theSameStudentTwiceInOneRequestIsCheckedInOnce() {
        enrolled("a", Enrollment.Status.ACTIVE);

        useCase.execute("school", List.of("a", "a"), false, "user");

        verify(visitRepo, times(1)).save(any());
        verify(studentRepo, times(1)).findByIdAndSchoolId(anyString(), eq("school"));
    }

    @Test
    void aStudentWhoseClassIsInTheLibraryNowIsCheckedInWithAWarning() {
        enrolled("a", Enrollment.Status.ACTIVE);
        LocalDate today = LocalDate.of(2026, 10, 29);
        var slot = new LibrarySession.Slot("entry", today, "cs-1", "JSS 2 A", "lib", "Library", "P2",
                LocalTime.of(8, 50), LocalTime.of(9, 30), null, "term", today.plusDays(30));
        var session = LibrarySession.open("school", slot, "user", "Librarian", today, NOW);
        var onRegister = LibrarySessionRecord.onRoster("school", session.getId(), "a", "Student a", "JSS 2", false);
        when(sessionRepo.findBetween("school", today, today)).thenReturn(List.of(session));
        when(recordRepo.findBySessions(any(), eq("school"))).thenReturn(List.of(onRegister));

        var res = useCase.execute("school", List.of("a"), false, "user");

        assertThat(res.checkedIn()).hasSize(1);
        assertThat(res.warnings()).singleElement()
                .satisfies(w -> assertThat(w.reason()).contains("JSS 2 A's library period now"));
    }
}
