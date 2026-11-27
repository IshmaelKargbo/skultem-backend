package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.domain.vo.Level;

// Both gender reports measure every enrolled student against the school days so far, like the students list.
class GenderAttendanceSchoolDaysTest {

    private final AttendanceRepository attendanceRepo = mock(AttendanceRepository.class);
    private final EnrollmentRepository enrollmentRepo = mock(EnrollmentRepository.class);
    private final ResolveAcademicYearUseCase years = mock(ResolveAcademicYearUseCase.class);
    private final ResolveTermSchoolDaysUseCase schoolDays = mock(ResolveTermSchoolDaysUseCase.class);
    private final SectionScopeService scope = mock(SectionScopeService.class);

    private final Clazz clazz = mock(Clazz.class);
    private final Section section = mock(Section.class);

    GenderAttendanceSchoolDaysTest() {
        var year = mock(AcademicYear.class);
        when(year.getId()).thenReturn("y");
        when(years.execute(anyString(), any())).thenReturn(year);
        when(scope.levels()).thenReturn(List.of(Level.PRIMARY));
        when(clazz.getId()).thenReturn("c1");
        when(clazz.getName()).thenReturn("Class 1");
        when(clazz.getLevel()).thenReturn(Level.PRIMARY);
        when(section.getId()).thenReturn("sec");
        when(section.getName()).thenReturn("All");
    }

    private Enrollment enrollment(String id, Gender gender) {
        var student = mock(Student.class);
        when(student.getId()).thenReturn("s-" + id);
        when(student.getGender()).thenReturn(gender);
        var e = mock(Enrollment.class);
        when(e.getId()).thenReturn(id);
        when(e.getStatus()).thenReturn(Enrollment.Status.ACTIVE);
        when(e.getClazz()).thenReturn(clazz);
        when(e.getSection()).thenReturn(section);
        when(e.getStudent()).thenReturn(student);
        return e;
    }

    private List<LocalDate> days(LocalDate from, int n) {
        return java.util.stream.IntStream.range(0, n).mapToObj(from::plusDays).toList();
    }

    @Test
    void summaryCountsAStudentNeverMarkedAsMissingEverySchoolDay() {
        var useCase = new GenerateGenderAttendanceSummaryUseCase(attendanceRepo, mock(TermRepository.class), years,
                scope, schoolDays, enrollmentRepo);
        when(schoolDays.between(eq("s"), eq("y"), any(), any())).thenReturn(days(LocalDate.of(2026, 11, 2), 10));
        // The boy was present all 10 days; the girl is enrolled but was never marked.
        when(attendanceRepo.attendanceCountsByClassAndGender(eq("s"), eq("y"), any(), any(), any(), any(), any(),
                any())).thenReturn(List.<Object[]>of(
                        new Object[] { "c1", "Class 1", "sec", "All", null, null, Gender.MALE, 10L, 10L, "e1" }));
        var boy = enrollment("e1", Gender.MALE);
        var girl = enrollment("e2", Gender.FEMALE);
        when(enrollmentRepo.findAllByAcademicSchoolId("y", "s")).thenReturn(List.of(boy, girl));

        var totals = useCase.execute("s", null, GenerateGenderAttendanceSummaryUseCase.Period.MONTH,
                LocalDate.of(2026, 11, 15), null, null, null, null, null).totals();

        assertThat(totals.boysAttendancePercentage()).isEqualTo(100.0);
        assertThat(totals.girlsRecorded()).isEqualTo(10);
        assertThat(totals.girlsAttendancePercentage()).isEqualTo(0.0);
        assertThat(totals.overallAttendancePercentage()).isEqualTo(50.0);
    }

    @Test
    void weeklyReportLeavesOutDaysThatAreNotSchoolDays() {
        var classRepo = mock(ClassRepository.class);
        when(classRepo.findByIdAndSchool("c1", "s")).thenReturn(Optional.of(clazz));
        var useCase = new GenerateWeeklyGenderAttendanceReportUseCase(classRepo, enrollmentRepo, attendanceRepo, years,
                schoolDays);
        var boy = enrollment("e1", Gender.MALE);
        when(enrollmentRepo.findAllByClassAndAcademicAndSchoolId(eq("c1"), eq("y"), eq("s"), any()))
                .thenReturn(new PageImpl<>(List.of(boy)));
        LocalDate mon = LocalDate.of(2026, 11, 2);
        // Wednesday is a no-school day: four school days, the boy present on all four.
        when(schoolDays.between(eq("s"), eq("y"), any(), any()))
                .thenReturn(List.of(mon, mon.plusDays(1), mon.plusDays(3), mon.plusDays(4)));
        when(attendanceRepo.attendanceCountsByClassGenderAndDateRange(eq("s"), eq("c1"), any(), any(), eq("y"), any(),
                any())).thenReturn(List.of(
                        new Object[] { mon, Gender.MALE, 1L, 1L },
                        new Object[] { mon.plusDays(1), Gender.MALE, 1L, 1L },
                        new Object[] { mon.plusDays(3), Gender.MALE, 1L, 1L },
                        new Object[] { mon.plusDays(4), Gender.MALE, 1L, 1L }));

        var report = useCase.execute("s", "c1", null, null, null, mon);

        var wed = report.days().get(2);
        assertThat(wed.schoolDay()).isFalse();
        assertThat(wed.boysAbsent()).isZero();
        assertThat(report.boysAttendancePercentage()).isEqualTo(100.0);
    }
}
