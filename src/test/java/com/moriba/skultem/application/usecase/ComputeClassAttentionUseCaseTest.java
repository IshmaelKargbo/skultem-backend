package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.domain.vo.Owner;

// Locks in how ComputeClassAttentionUseCase turns a school's settings into attendance flags: the
// threshold, window, minimum recorded days and absence streak all come from the school itself
// (School#attendanceRules) rather than constants, and the rates are over recorded school days.
@ExtendWith(MockitoExtension.class)
class ComputeClassAttentionUseCaseTest {

    private static final String SCHOOL_ID = "school-1";
    private static final String CLASS_ID = "class-1";
    private static final String ACADEMIC_YEAR_ID = "year-1";
    private static final String ENROLLMENT_ID = "enrollment-1";

    @Mock
    private ClassRepository classRepo;
    @Mock
    private TermRepository termRepo;
    @Mock
    private EnrollmentRepository enrollmentRepo;
    @Mock
    private AttendanceRepository attendanceRepo;
    @Mock
    private AssessmentScoreRepository scoreRepo;
    @Mock
    private SchoolRepository schoolRepo;
    @Mock
    private ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    @Mock
    private com.moriba.skultem.application.services.AttendanceRulesResolver attendanceRulesResolver;

    private ComputeClassAttentionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ComputeClassAttentionUseCase(classRepo, termRepo, enrollmentRepo, attendanceRepo, scoreRepo,
                schoolRepo, resolveAcademicYearUseCase, attendanceRulesResolver);

        var clazz = Clazz.create(CLASS_ID, SCHOOL_ID, null, "Primary 5", Level.PRIMARY, 1);
        var academicYear = AcademicYear.create(ACADEMIC_YEAR_ID, SCHOOL_ID, "2025/2026",
                LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6));
        var student = Student.create(SCHOOL_ID, null, "A-001", LocalDate.now(), "Ama", "Kamara", null, null, null,
                null, null, LocalDate.now().minusYears(10), null, null, null, "Sierra Leonean", null, "Freetown",
                null);
        var enrollment = Enrollment.create(ENROLLMENT_ID, SCHOOL_ID, student, clazz, null, academicYear, null);

        when(classRepo.findByIdAndSchool(CLASS_ID, SCHOOL_ID)).thenReturn(Optional.of(clazz));
        when(resolveAcademicYearUseCase.execute(SCHOOL_ID, null)).thenReturn(academicYear);
        when(termRepo.findActiveBySchoolAndAcademicYear(SCHOOL_ID, ACADEMIC_YEAR_ID)).thenReturn(Optional.empty());
        when(enrollmentRepo.findAllByClassAndAcademicAndSchoolId(CLASS_ID, ACADEMIC_YEAR_ID, SCHOOL_ID,
                Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(enrollment)));
        // No active term is stubbed, so the use case's academic-score branch is never entered -
        // scoreRepo is intentionally left unstubbed here.
    }

    // The school itself, plus the rules the resolver reports for this class's level (no section
    // override unless a test passes some).
    private void useSchool(School school) {
        useSchool(school, school.attendanceRules());
    }

    private void useSchool(School school, com.moriba.skultem.domain.service.AttendanceAttentionCalculator.Rules rules) {
        when(schoolRepo.findById(SCHOOL_ID)).thenReturn(Optional.of(school));
        when(attendanceRulesResolver.forLevel(any(), eq(Level.PRIMARY))).thenReturn(rules);
    }

    private School schoolWithThreshold(double threshold) {
        var school = School.create(SCHOOL_ID, "Test School", "test.skultem.com",
                new Address("Western Area", "Freetown", "Freetown", "Freetown", "1 Main St"),
                new Owner("Jane", "Doe", "jane@example.com", "+23276000000"));
        school.update(school.getName(), school.getDomain(), school.getAddress(), threshold,
                School.GenderComposition.MIXED);
        return school;
    }

    // pattern is oldest -> newest ('P' present, 'A' absent), one recorded day each, ending today.
    private void stubDays(String pattern) {
        List<Object[]> rows = new java.util.ArrayList<>();
        for (int i = 0; i < pattern.length(); i++) {
            rows.add(new Object[] { ENROLLMENT_ID, LocalDate.now().minusDays(pattern.length() - 1 - i),
                    pattern.charAt(i) == 'P' ? 1 : 0 });
        }
        when(attendanceRepo.attendanceDaysSince(eq(SCHOOL_ID), eq(CLASS_ID), eq(ACADEMIC_YEAR_ID), any()))
                .thenReturn(rows);
    }

    @Test
    void aStudentBelowTheDefaultSeventyFivePercentThresholdIsFlagged() {
        // 12/20 = 60%, below the default 75% bar (no earlier window, so no trend to excuse it).
        useSchool(schoolWithThreshold(75.0));
        stubDays("PAPAP".repeat(4));

        var result = useCase.execute(SCHOOL_ID, CLASS_ID, null);

        assertThat(result.students()).hasSize(1);
        var flagged = result.students().get(0);
        assertThat(flagged.attendanceFlag()).isTrue();
        assertThat(flagged.attendanceRate()).isLessThan(75.0);
    }

    @Test
    void theSameStudentIsNotFlaggedWhenTheSchoolConfiguresALowerThreshold() {
        useSchool(schoolWithThreshold(40.0));
        stubDays("PAPAP".repeat(4)); // 60%, above the school's 40% bar

        var result = useCase.execute(SCHOOL_ID, CLASS_ID, null);

        assertThat(result.students()).isEmpty();
    }

    @Test
    void tooFewRecordedDaysIsNotFlaggedOnRate() {
        useSchool(schoolWithThreshold(75.0));
        stubDays("PAPA"); // 4 days: under the school's minimum of 5

        var result = useCase.execute(SCHOOL_ID, CLASS_ID, null);

        assertThat(result.students()).isEmpty();
    }

    @Test
    void theSchoolConfiguredStreakMakesAStudentCritical() {
        var school = schoolWithThreshold(75.0);
        school.updateAttendanceRules(null, null, 2); // two absences in a row is already critical here
        useSchool(school);
        stubDays("PPPPPPPPPPPPPPPPPPAA");

        var result = useCase.execute(SCHOOL_ID, CLASS_ID, null);

        assertThat(result.students()).hasSize(1);
        assertThat(result.students().get(0).severity())
                .isEqualTo(com.moriba.skultem.domain.service.AttendanceAttentionCalculator.Level.CRITICAL);
        assertThat(result.students().get(0).absenceStreak()).isEqualTo(2);
    }

    @Test
    void aStudentWhoHasRecoveredIsNoLongerFlagged() {
        useSchool(schoolWithThreshold(75.0));
        // 20 days at 40%, then 20 days at 90%: the recent window is above the bar.
        stubDays("PAPAA".repeat(4) + "PPPPPPPPPA".repeat(2));

        var result = useCase.execute(SCHOOL_ID, CLASS_ID, null);

        assertThat(result.students()).isEmpty();
        assertThat(result.flaggedCount()).isEqualTo(0);
    }

    @Test
    void aSectionsOwnRulesOverrideTheSchoolsForItsClasses() {
        // The school's bar is 40%, so 60% attendance is fine for it - but this class's section sets 75%.
        var school = schoolWithThreshold(40.0);
        var sectionRules = new com.moriba.skultem.domain.service.AttendanceAttentionCalculator.Rules(75.0, 20, 5, 3);
        useSchool(school, sectionRules);
        stubDays("PAPAP".repeat(4)); // 60%

        var result = useCase.execute(SCHOOL_ID, CLASS_ID, null);

        assertThat(result.students()).hasSize(1);
        assertThat(result.students().get(0).attendanceFlag()).isTrue();
    }
}
