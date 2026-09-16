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

// Locks in ComputeClassAttentionUseCase's migration onto AttendanceRateCalculator: the "needs
// attention" rate math must stay byte-for-byte identical to the pre-refactor inline formula
// (characterization), and the flagging boundary must now come from the school's own configured
// attendanceThreshold rather than a fixed 75 - this is the one behavior change intended by that
// refactor, not a side effect of it.
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

    private ComputeClassAttentionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ComputeClassAttentionUseCase(classRepo, termRepo, enrollmentRepo, attendanceRepo, scoreRepo,
                schoolRepo, resolveAcademicYearUseCase);

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

    private School schoolWithThreshold(double threshold) {
        var school = School.create(SCHOOL_ID, "Test School", "test.skultem.com",
                new Address("Western Area", "Freetown", "Freetown", "Freetown", "1 Main St"),
                new Owner("Jane", "Doe", "jane@example.com", "+23276000000"));
        school.update(school.getName(), school.getDomain(), school.getAddress(), threshold);
        return school;
    }

    private void stubAttendanceCounts(long presentOrLate, long total) {
        when(attendanceRepo.attendanceCountsByClassSince(eq(SCHOOL_ID), eq(CLASS_ID), eq(ACADEMIC_YEAR_ID), any()))
                .thenReturn(List.<Object[]>of(new Object[] { ENROLLMENT_ID, presentOrLate, total }));
    }

    @Test
    void aStudentBelowTheDefaultSeventyFivePercentThresholdIsFlagged() {
        // 22/30 = 73.3%, below the default 75% threshold - same fraction the old inline
        // Math.round((22.0/30.0) * 1000.0) / 10.0 formula produced.
        when(schoolRepo.findById(SCHOOL_ID)).thenReturn(Optional.of(schoolWithThreshold(75.0)));
        stubAttendanceCounts(22, 30);

        var result = useCase.execute(SCHOOL_ID, CLASS_ID, null);

        assertThat(result.flaggedCount()).isEqualTo(1);
        var flagged = result.students().get(0);
        assertThat(flagged.attendanceRate()).isEqualTo(73.3);
        assertThat(flagged.attendanceFlag()).isTrue();
    }

    @Test
    void theSameStudentIsNotFlaggedWhenTheSchoolConfiguresALowerThreshold() {
        // 73.3% clears a school-configured 70% bar, even though it fails the default 75% one.
        when(schoolRepo.findById(SCHOOL_ID)).thenReturn(Optional.of(schoolWithThreshold(70.0)));
        stubAttendanceCounts(22, 30);

        var result = useCase.execute(SCHOOL_ID, CLASS_ID, null);

        assertThat(result.flaggedCount()).isEqualTo(0);
    }

    @Test
    void aHigherSchoolConfiguredThresholdFlagsAStudentTheDefaultWouldNotHave() {
        // 78% clears the default 75% bar but fails an 80% school-configured one.
        when(schoolRepo.findById(SCHOOL_ID)).thenReturn(Optional.of(schoolWithThreshold(80.0)));
        stubAttendanceCounts(78, 100);

        var result = useCase.execute(SCHOOL_ID, CLASS_ID, null);

        assertThat(result.flaggedCount()).isEqualTo(1);
        assertThat(result.students().get(0).attendanceRate()).isEqualTo(78.0);
    }
}
