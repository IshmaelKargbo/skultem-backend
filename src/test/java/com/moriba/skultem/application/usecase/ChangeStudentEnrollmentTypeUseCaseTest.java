package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.FeeCategory;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.Student.EnrollmentType;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;
import com.moriba.skultem.domain.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class ChangeStudentEnrollmentTypeUseCaseTest {

    private static final String SCHOOL = "school-1";
    private static final String STUDENT = "student-1";

    @Mock
    private StudentRepository studentRepo;
    @Mock
    private AcademicYearRepository academicYearRepo;
    @Mock
    private EnrollmentRepository enrollmentRepo;
    @Mock
    private StudentFeeRepository studentFeeRepo;
    @Mock
    private PaymentRepository paymentRepo;
    @Mock
    private FeeDiscountRepository feeDiscountRepo;
    @Mock
    private StudentLedgerEntryRepository ledgerRepo;
    @Mock
    private RecomputeStudentLedgerBalancesUseCase recomputeLedgerBalances;
    @Mock
    private ApplyApplicableFeesToEnrollmentUseCase applyApplicableFees;
    @Mock
    private LogActivityUseCase logActivityUseCase;
    @InjectMocks
    private ChangeStudentEnrollmentTypeUseCase useCase;

    private Student student;
    private Enrollment enrollment;
    private StudentFee uniformFee;

    @BeforeEach
    void setUp() {
        student = mock(Student.class);
        lenient().when(student.getId()).thenReturn(STUDENT);
        lenient().when(student.getName()).thenReturn("Aminata Kamara");
        lenient().when(student.getEnrollmentType()).thenReturn(EnrollmentType.NEW);
        lenient().when(studentRepo.findByIdAndSchoolId(STUDENT, SCHOOL)).thenReturn(Optional.of(student));

        var year = mock(AcademicYear.class);
        lenient().when(year.getId()).thenReturn("year-1");
        lenient().when(year.isLocked()).thenReturn(false);
        lenient().when(academicYearRepo.findActiveBySchool(SCHOOL)).thenReturn(Optional.of(year));

        enrollment = mock(Enrollment.class);
        lenient().when(enrollment.getId()).thenReturn("enrollment-1");
        lenient().when(enrollment.getStatus()).thenReturn(Enrollment.Status.ACTIVE);
        lenient().when(enrollmentRepo.findByStudentAndAcademicYearAndSchoolId(STUDENT, "year-1", SCHOOL))
                .thenReturn(Optional.of(enrollment));
        lenient().when(enrollmentRepo.findByIdAndSchoolId("enrollment-1", SCHOOL)).thenReturn(Optional.of(enrollment));

        // A "Uniform" fee that only new students are charged.
        var fee = mock(FeeStructure.class);
        var category = mock(FeeCategory.class);
        var term = mock(Term.class);
        lenient().when(category.getName()).thenReturn("Uniform");
        lenient().when(term.getName()).thenReturn("Term 1");
        lenient().when(fee.getId()).thenReturn("fee-uniform");
        lenient().when(fee.getCategory()).thenReturn(category);
        lenient().when(fee.getTerm()).thenReturn(term);
        lenient().when(fee.isNewStudentsOnly()).thenReturn(true);
        lenient().when(fee.getAmount()).thenReturn(new BigDecimal("150"));
        uniformFee = mock(StudentFee.class);
        lenient().when(uniformFee.getFee()).thenReturn(fee);
        lenient().when(studentFeeRepo.findBySchoolAndEnrollment(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(uniformFee)));
        lenient().when(paymentRepo.sumPaymentsByStudentAndFee(STUDENT, "fee-uniform")).thenReturn(BigDecimal.ZERO);
        lenient().when(feeDiscountRepo.findBySchoolAndStudentIdAndFeeId(SCHOOL, STUDENT, "fee-uniform"))
                .thenReturn(List.of());
        lenient().when(ledgerRepo.findAllByStudentIdAndSchoolIdOrderByPaidAtAscCreatedAtAsc(STUDENT, SCHOOL))
                .thenReturn(List.of());
    }

    @Test
    void movingFromNewToReEnrolmentTakesOffAnUnpaidNewStudentsFeeAndChargesTheRest() {
        var result = useCase.execute(SCHOOL, STUDENT, EnrollmentType.RE_ENROLLMENT);

        assertThat(result.feesRemoved()).containsExactly("Uniform (Term 1)");
        verify(studentFeeRepo).deleteByEnrollmentAndFeeAndSchool("enrollment-1", "fee-uniform", SCHOOL);
        verify(student).changeEnrollmentType(EnrollmentType.RE_ENROLLMENT);
        verify(applyApplicableFees).execute(enrollment);
        verify(recomputeLedgerBalances).recomputeForStudent(STUDENT, SCHOOL);
    }

    @Test
    void aNewStudentsFeeWithAPaymentOnItBlocksTheChangeAndNothingIsTouched() {
        when(paymentRepo.sumPaymentsByStudentAndFee(STUDENT, "fee-uniform")).thenReturn(new BigDecimal("150"));

        assertThatThrownBy(() -> useCase.execute(SCHOOL, STUDENT, EnrollmentType.RE_ENROLLMENT))
                .isInstanceOf(RuleException.class)
                .hasMessageContaining("Uniform (Term 1)")
                .hasMessageContaining("has payments");

        verify(student, never()).changeEnrollmentType(any());
        verify(studentFeeRepo, never()).deleteByEnrollmentAndFeeAndSchool(anyString(), anyString(), anyString());
        verify(applyApplicableFees, never()).execute(any());
    }

    @Test
    void aNewStudentsFeeWithADiscountBlocksTheChange() {
        when(uniformFee.getDiscount()).thenReturn(mock(com.moriba.skultem.domain.model.FeeDiscount.class));

        assertThatThrownBy(() -> useCase.execute(SCHOOL, STUDENT, EnrollmentType.EXISTING))
                .isInstanceOf(RuleException.class).hasMessageContaining("has a discount");
        verify(student, never()).changeEnrollmentType(any());
    }

    @Test
    void changingWithinTheSameGroupOnlyUpdatesTheType() {
        var result = useCase.execute(SCHOOL, STUDENT, EnrollmentType.TRANSFER);

        assertThat(result.feesAdded()).isEmpty();
        assertThat(result.feesRemoved()).isEmpty();
        verify(student).changeEnrollmentType(EnrollmentType.TRANSFER);
        verify(studentFeeRepo, never()).deleteByEnrollmentAndFeeAndSchool(anyString(), anyString(), anyString());
        verify(applyApplicableFees, never()).execute(any());
    }

    @Test
    void choosingTheTypeTheStudentAlreadyHasIsRefused() {
        assertThatThrownBy(() -> useCase.execute(SCHOOL, STUDENT, EnrollmentType.NEW))
                .isInstanceOf(RuleException.class).hasMessageContaining("already");
        verify(student, never()).changeEnrollmentType(any());
    }

    @Test
    void aStudentWithNoEnrollmentThisYearJustGetsTheNewType() {
        when(enrollmentRepo.findByStudentAndAcademicYearAndSchoolId(STUDENT, "year-1", SCHOOL))
                .thenReturn(Optional.empty());

        useCase.execute(SCHOOL, STUDENT, EnrollmentType.RE_ENROLLMENT);

        verify(student).changeEnrollmentType(EnrollmentType.RE_ENROLLMENT);
        verify(applyApplicableFees, never()).execute(any());
    }
}
