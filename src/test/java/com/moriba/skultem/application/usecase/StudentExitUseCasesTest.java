package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentPurgeRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.Gender;

@ExtendWith(MockitoExtension.class)
class StudentExitUseCasesTest {

    private static final String SCHOOL = "school-1";

    @Mock
    private StudentRepository studentRepo;
    @Mock
    private EnrollmentRepository enrollmentRepo;
    @Mock
    private GetStudentUseCase getStudentUseCase;
    @Mock
    private LogActivityUseCase logActivityUseCase;
    @Mock
    private StudentPurgeRepository purgeRepo;
    @InjectMocks
    private EndStudentEnrollmentUseCase endUseCase;

    private Student student;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        student = Student.create(SCHOOL, null, "ADM-001", LocalDate.now(), "Ama", "Kamara", null, null, null,
                Gender.FEMALE, null, null, Student.EnrollmentType.NEW, null, null, null, null, null, null);
        enrollment = Enrollment.create("enr-1", SCHOOL, student, null, null, null, null);
        org.mockito.Mockito.lenient().when(studentRepo.findByIdAndSchoolId(student.getId(), SCHOOL))
                .thenReturn(Optional.of(student));
    }

    @Test
    void withdrawingMarksTheStudentAndEndsTheirActiveEnrollments() {
        when(enrollmentRepo.findAllByStudentAndSchoolIdAndStatus(student.getId(), SCHOOL, Enrollment.Status.ACTIVE))
                .thenReturn(List.of(enrollment));

        endUseCase.execute(SCHOOL, student.getId(), Student.Status.WITHDRAWN, "Relocated", null, "  moving abroad ");

        assertThat(student.getStatus()).isEqualTo(Student.Status.WITHDRAWN);
        assertThat(student.getExitReason()).isEqualTo("Relocated");
        assertThat(student.getExitNote()).isEqualTo("moving abroad");
        assertThat(student.getExitDate()).isEqualTo(LocalDate.now());
        assertThat(enrollment.getStatus()).isEqualTo(Enrollment.Status.LEFT);
        verify(enrollmentRepo).save(enrollment);
    }

    @Test
    void expellingNeedsAReason() {
        assertThatThrownBy(() -> endUseCase.execute(SCHOOL, student.getId(), Student.Status.EXPELLED, "  ", null, null))
                .isInstanceOf(RuleException.class).hasMessageContaining("reason");
        assertThat(student.getStatus()).isEqualTo(Student.Status.ACTIVE);
        verify(studentRepo, never()).save(any());
    }

    @Test
    void aFutureDateIsRejected() {
        assertThatThrownBy(() -> endUseCase.execute(SCHOOL, student.getId(), Student.Status.WITHDRAWN, null,
                LocalDate.now().plusDays(1), null)).isInstanceOf(RuleException.class);
    }

    @Test
    void aStudentWhoAlreadyLeftCantBeWithdrawnAgain() {
        student.leaveSchool(Student.Status.EXPELLED, "Misconduct", LocalDate.now(), null);

        assertThatThrownBy(() -> endUseCase.execute(SCHOOL, student.getId(), Student.Status.WITHDRAWN, null, null, null))
                .isInstanceOf(RuleException.class).hasMessageContaining("already");
    }

    @Test
    void onlyWithdrawnOrExpelledAreAccepted() {
        assertThatThrownBy(() -> endUseCase.execute(SCHOOL, student.getId(), Student.Status.GRADUATED, null, null, null))
                .isInstanceOf(RuleException.class);
    }

    @Test
    void reinstatingClearsTheExitAndReactivatesTheCurrentYearEnrollment() {
        student.leaveSchool(Student.Status.WITHDRAWN, "Relocated", LocalDate.now(), "note");
        enrollment.leave();
        var academicYearRepo = org.mockito.Mockito.mock(com.moriba.skultem.domain.repository.AcademicYearRepository.class);
        var year = org.mockito.Mockito.mock(com.moriba.skultem.domain.model.AcademicYear.class);
        when(year.getId()).thenReturn("ay-1");
        when(academicYearRepo.findActiveBySchool(SCHOOL)).thenReturn(Optional.of(year));
        var leftEnrollment = org.mockito.Mockito.spy(enrollment);
        var enrollmentYear = org.mockito.Mockito.mock(com.moriba.skultem.domain.model.AcademicYear.class);
        when(enrollmentYear.getId()).thenReturn("ay-1");
        when(leftEnrollment.getAcademicYear()).thenReturn(enrollmentYear);
        when(enrollmentRepo.findAllByStudentAndSchoolIdAndStatus(student.getId(), SCHOOL, Enrollment.Status.LEFT))
                .thenReturn(List.of(leftEnrollment));

        new ReinstateStudentUseCase(studentRepo, enrollmentRepo, academicYearRepo, getStudentUseCase,
                logActivityUseCase).execute(SCHOOL, student.getId());

        assertThat(student.getStatus()).isEqualTo(Student.Status.ACTIVE);
        assertThat(student.getExitReason()).isNull();
        assertThat(student.getExitDate()).isNull();
        assertThat(leftEnrollment.getStatus()).isEqualTo(Enrollment.Status.ACTIVE);
    }

    @Test
    void permanentDeleteNeedsTheAdmissionNumber() {
        var delete = new DeleteStudentPermanentlyUseCase(studentRepo, purgeRepo, logActivityUseCase);

        assertThatThrownBy(() -> delete.execute(SCHOOL, student.getId(), "wrong"))
                .isInstanceOf(RuleException.class).hasMessageContaining("ADM-001");
        verify(purgeRepo, never()).purge(anyString(), anyString());
    }

    @Test
    void permanentDeleteIsRefusedOnceMoneyWasCollected() {
        when(purgeRepo.hasRecordedPayments(SCHOOL, student.getId())).thenReturn(true);
        var delete = new DeleteStudentPermanentlyUseCase(studentRepo, purgeRepo, logActivityUseCase);

        assertThatThrownBy(() -> delete.execute(SCHOOL, student.getId(), "adm-001"))
                .isInstanceOf(RuleException.class).hasMessageContaining("payments");
        verify(purgeRepo, never()).purge(anyString(), anyString());
    }

    @Test
    void permanentDeletePurgesEverythingAndReportsWhatWentWithIt() {
        when(purgeRepo.hasRecordedPayments(SCHOOL, student.getId())).thenReturn(false);
        when(purgeRepo.purge(SCHOOL, student.getId())).thenReturn(new StudentPurgeRepository.Purged(1, 3, 12));
        var delete = new DeleteStudentPermanentlyUseCase(studentRepo, purgeRepo, logActivityUseCase);

        var result = delete.execute(SCHOOL, student.getId(), " ADM-001 ");

        assertThat(result.feesRemoved()).isEqualTo(3);
        assertThat(result.assessmentsRemoved()).isEqualTo(12);
        verify(purgeRepo).purge(SCHOOL, student.getId());
    }
}
