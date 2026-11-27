package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.domain.vo.Owner;

// Locks in the school-wide grouping/aggregation logic: per-class session rows (not pre-aggregated
// by the query) are grouped in Java into one row per class, with independent Present/Absent/Late,
// gender and threshold totals - "SSS 1 Science" and "SSS 1 Art" must never bleed into each other.
@ExtendWith(MockitoExtension.class)
class GenerateClassAttendanceSummaryUseCaseTest {

    private static final String SCHOOL_ID = "school-1";
    private static final String ACADEMIC_YEAR_ID = "year-1";

    @Mock
    private SchoolRepository schoolRepo;
    @Mock
    private TermRepository termRepo;
    @Mock
    private AttendanceRepository attendanceRepo;
    @Mock
    private ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    @Mock
    private com.moriba.skultem.domain.repository.ClassRepository classRepo;
    @Mock
    private com.moriba.skultem.application.services.SectionScopeService sectionScopeService;
    @Mock
    private com.moriba.skultem.application.services.AttendanceRulesResolver attendanceRulesResolver;
    @Mock
    private ResolveTermSchoolDaysUseCase resolveTermSchoolDaysUseCase;
    @Mock
    private com.moriba.skultem.domain.repository.EnrollmentRepository enrollmentRepo;

    private GenerateClassAttendanceSummaryUseCase useCase;

    private School schoolWithThreshold(double threshold) {
        var school = School.create(SCHOOL_ID, "Test School", "test.skultem.com",
                new Address("Western Area", "Freetown", "Freetown", "Freetown", "1 Main St"),
                new Owner("Jane", "Doe", "jane@example.com", "+23276000000"));
        school.update(school.getName(), school.getDomain(), school.getAddress(), threshold,
                School.GenderComposition.MIXED);
        return school;
    }

    private Object[] row(String classId, String className, String sectionId, String sectionName, String streamId,
            String streamName, String enrollmentId, String studentId, String gender, long presentOrLate, long late,
            long totalRecorded) {
        return new Object[] { classId, className, sectionId, sectionName, streamId, streamName, enrollmentId,
                studentId, gender, presentOrLate, late, totalRecorded };
    }

    @Test
    void groupsRowsIntoOneEntryPerClassSessionKeepingScienceAndArtSeparate() {
        org.mockito.Mockito.lenient().when(sectionScopeService.effective())
                .thenReturn(com.moriba.skultem.domain.vo.SectionScope.all());
        // No per-section overrides here: every class falls back to the school's own threshold.
        org.mockito.Mockito.lenient().when(classRepo.findBySchool(eq(SCHOOL_ID),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        useCase = new GenerateClassAttendanceSummaryUseCase(schoolRepo, termRepo, attendanceRepo,
                resolveAcademicYearUseCase, classRepo, sectionScopeService, attendanceRulesResolver,
                resolveTermSchoolDaysUseCase, enrollmentRepo);

        var academicYear = AcademicYear.create(ACADEMIC_YEAR_ID, SCHOOL_ID, "2025/2026",
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 7, 31));
        when(resolveAcademicYearUseCase.execute(SCHOOL_ID, null)).thenReturn(academicYear);
        when(schoolRepo.findById(SCHOOL_ID)).thenReturn(Optional.of(schoolWithThreshold(75.0)));

        // 20 school days so far - what every student is measured against.
        when(resolveTermSchoolDaysUseCase.between(eq(SCHOOL_ID), eq(ACADEMIC_YEAR_ID), any(), any()))
                .thenReturn(java.util.stream.IntStream.range(0, 20)
                        .mapToObj(i -> LocalDate.of(2025, 9, 1).plusDays(i)).toList());

        // SSS 1 Science: one boy at 100%, one girl at 40% - class average 70%, below threshold.
        // SSS 1 Art: one boy at 90%.
        when(attendanceRepo.attendanceCountsBySchoolAndDateRange(eq(SCHOOL_ID), eq(ACADEMIC_YEAR_ID), any(), any()))
                .thenReturn(List.of(
                        row("class-1", "SSS 1", "sec-1", "All", "stream-sci", "Science", "e1", "s1", "MALE", 20, 0,
                                20),
                        row("class-1", "SSS 1", "sec-1", "All", "stream-sci", "Science", "e2", "s2", "FEMALE", 8, 0,
                                20),
                        row("class-1", "SSS 1", "sec-1", "All", "stream-art", "Art", "e3", "s3", "MALE", 18, 2, 20)));

        var result = useCase.execute(SCHOOL_ID, null, null);

        assertThat(result.totalClasses()).isEqualTo(2);
        assertThat(result.termLabel()).isNull();

        var science = result.classes().stream().filter(c -> "Science".equals(extractStream(c.className())))
                .findFirst().orElseThrow();
        assertThat(science.totalStudents()).isEqualTo(2);
        assertThat(science.totalBoys()).isEqualTo(1);
        assertThat(science.totalGirls()).isEqualTo(1);
        assertThat(science.attendancePercentage()).isEqualTo(70.0); // (20+8)/40
        assertThat(science.belowThreshold()).isTrue();

        var art = result.classes().stream().filter(c -> "Art".equals(extractStream(c.className()))).findFirst()
                .orElseThrow();
        assertThat(art.totalStudents()).isEqualTo(1);
        assertThat(art.attendancePercentage()).isEqualTo(90.0); // (18+2)/20 - late counts as attended
        assertThat(art.belowThreshold()).isFalse();

        assertThat(result.schoolDays()).isEqualTo(20);
        assertThat(result.classesBelowThreshold()).isEqualTo(1);
        assertThat(result.totalStudents()).isEqualTo(3);
        assertThat(result.totalBoys()).isEqualTo(2);
        assertThat(result.totalGirls()).isEqualTo(1);
    }

    private String extractStream(String label) {
        return label.substring(label.indexOf('-') + 2);
    }

    @Test
    void aSpecificTermUsesTheTermsOwnDateRangeAndNameInsteadOfTheWholeYear() {
        org.mockito.Mockito.lenient().when(sectionScopeService.effective())
                .thenReturn(com.moriba.skultem.domain.vo.SectionScope.all());
        // No per-section overrides here: every class falls back to the school's own threshold.
        org.mockito.Mockito.lenient().when(classRepo.findBySchool(eq(SCHOOL_ID),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        useCase = new GenerateClassAttendanceSummaryUseCase(schoolRepo, termRepo, attendanceRepo,
                resolveAcademicYearUseCase, classRepo, sectionScopeService, attendanceRulesResolver,
                resolveTermSchoolDaysUseCase, enrollmentRepo);

        var academicYear = AcademicYear.create(ACADEMIC_YEAR_ID, SCHOOL_ID, "2025/2026",
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 7, 31));
        when(resolveAcademicYearUseCase.execute(SCHOOL_ID, null)).thenReturn(academicYear);
        when(schoolRepo.findById(SCHOOL_ID)).thenReturn(Optional.of(schoolWithThreshold(75.0)));

        var term = Term.create("term-1", SCHOOL_ID, academicYear, "First Term", 1,
                Term.Status.ACTIVE, LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 15));
        when(termRepo.findByIdAndSchoolId("term-1", SCHOOL_ID)).thenReturn(Optional.of(term));

        when(attendanceRepo.attendanceCountsBySchoolAndDateRange(eq(SCHOOL_ID), eq(ACADEMIC_YEAR_ID),
                eq(LocalDate.of(2025, 9, 1)), eq(LocalDate.of(2025, 12, 15))))
                .thenReturn(List.of());

        var result = useCase.execute(SCHOOL_ID, null, "term-1");

        assertThat(result.termLabel()).isEqualTo("First Term");
        assertThat(result.totalClasses()).isEqualTo(0);
    }

    @Test
    void daysWithNoMarkCountAsAbsentAgainstTheSchoolDaysSoFar() {
        org.mockito.Mockito.lenient().when(sectionScopeService.effective())
                .thenReturn(com.moriba.skultem.domain.vo.SectionScope.all());
        org.mockito.Mockito.lenient().when(classRepo.findBySchool(eq(SCHOOL_ID),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        useCase = new GenerateClassAttendanceSummaryUseCase(schoolRepo, termRepo, attendanceRepo,
                resolveAcademicYearUseCase, classRepo, sectionScopeService, attendanceRulesResolver,
                resolveTermSchoolDaysUseCase, enrollmentRepo);

        var academicYear = AcademicYear.create(ACADEMIC_YEAR_ID, SCHOOL_ID, "2025/2026",
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 7, 31));
        when(resolveAcademicYearUseCase.execute(SCHOOL_ID, null)).thenReturn(academicYear);
        when(schoolRepo.findById(SCHOOL_ID)).thenReturn(Optional.of(schoolWithThreshold(75.0)));
        when(resolveTermSchoolDaysUseCase.between(eq(SCHOOL_ID), eq(ACADEMIC_YEAR_ID), any(), any()))
                .thenReturn(java.util.stream.IntStream.range(0, 10)
                        .mapToObj(i -> LocalDate.of(2025, 9, 1).plusDays(i)).toList());
        // Marked on 8 of the 10 days, present on all 8: 2 days never marked.
        when(attendanceRepo.attendanceCountsBySchoolAndDateRange(eq(SCHOOL_ID), eq(ACADEMIC_YEAR_ID), any(), any()))
                .thenReturn(List.<Object[]>of(row("class-1", "JSS 1", "sec-1", "All", null, null, "e1", "s1", "MALE", 8,
                        0, 8)));

        var jss = useCase.execute(SCHOOL_ID, null, null).classes().get(0);
        assertThat(jss.present()).isEqualTo(8);
        assertThat(jss.absent()).isEqualTo(2);
        assertThat(jss.attendancePercentage()).isEqualTo(80.0);
    }

    @Test
    void anEnrolledStudentNeverMarkedStillCountsMissingEverySchoolDay() {
        org.mockito.Mockito.lenient().when(sectionScopeService.effective())
                .thenReturn(com.moriba.skultem.domain.vo.SectionScope.all());
        org.mockito.Mockito.lenient().when(classRepo.findBySchool(eq(SCHOOL_ID),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        useCase = new GenerateClassAttendanceSummaryUseCase(schoolRepo, termRepo, attendanceRepo,
                resolveAcademicYearUseCase, classRepo, sectionScopeService, attendanceRulesResolver,
                resolveTermSchoolDaysUseCase, enrollmentRepo);

        var academicYear = AcademicYear.create(ACADEMIC_YEAR_ID, SCHOOL_ID, "2025/2026",
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 7, 31));
        when(resolveAcademicYearUseCase.execute(SCHOOL_ID, null)).thenReturn(academicYear);
        when(schoolRepo.findById(SCHOOL_ID)).thenReturn(Optional.of(schoolWithThreshold(75.0)));
        when(resolveTermSchoolDaysUseCase.between(eq(SCHOOL_ID), eq(ACADEMIC_YEAR_ID), any(), any()))
                .thenReturn(java.util.stream.IntStream.range(0, 10)
                        .mapToObj(i -> LocalDate.of(2025, 9, 1).plusDays(i)).toList());
        // e1 was present all 10 days; e2 is enrolled in the same class but was never marked.
        when(attendanceRepo.attendanceCountsBySchoolAndDateRange(eq(SCHOOL_ID), eq(ACADEMIC_YEAR_ID), any(), any()))
                .thenReturn(List.<Object[]>of(row("class-1", "JSS 1", "sec-1", "All", null, null, "e1", "s1", "MALE", 10,
                        0, 10)));
        var clazz = org.mockito.Mockito.mock(com.moriba.skultem.domain.model.Clazz.class);
        when(clazz.getId()).thenReturn("class-1");
        when(clazz.getName()).thenReturn("JSS 1");
        var section = org.mockito.Mockito.mock(com.moriba.skultem.domain.model.Section.class);
        when(section.getId()).thenReturn("sec-1");
        when(section.getName()).thenReturn("All");
        var student = org.mockito.Mockito.mock(com.moriba.skultem.domain.model.Student.class);
        when(student.getId()).thenReturn("s2");
        when(student.getGender()).thenReturn(com.moriba.skultem.domain.vo.Gender.FEMALE);
        var e2 = org.mockito.Mockito.mock(com.moriba.skultem.domain.model.Enrollment.class);
        when(e2.getId()).thenReturn("e2");
        when(e2.getStatus()).thenReturn(com.moriba.skultem.domain.model.Enrollment.Status.ACTIVE);
        when(e2.getClazz()).thenReturn(clazz);
        when(e2.getSection()).thenReturn(section);
        when(e2.getStudent()).thenReturn(student);
        when(enrollmentRepo.findAllByAcademicSchoolId(ACADEMIC_YEAR_ID, SCHOOL_ID)).thenReturn(List.of(e2));

        var result = useCase.execute(SCHOOL_ID, null, null);
        var jss = result.classes().get(0);
        assertThat(result.totalClasses()).isEqualTo(1);
        assertThat(jss.totalStudents()).isEqualTo(2);
        assertThat(jss.totalGirls()).isEqualTo(1);
        assertThat(jss.absent()).isEqualTo(10);
        assertThat(jss.attendancePercentage()).isEqualTo(50.0); // 10 of 20 student-days
    }
}
