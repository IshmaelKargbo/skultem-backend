package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.Attendance;
import com.moriba.skultem.domain.model.ClassSection;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.Payment;
import com.moriba.skultem.domain.model.Payment.PaymentMethod;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.BehaviourRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSectionRepository;
import com.moriba.skultem.domain.repository.ClassStreamRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.EnrollmentSubjectRepository;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;
import com.moriba.skultem.domain.vo.Level;

@ExtendWith(MockitoExtension.class)
class ChangeEnrollmentClassUseCaseTest {

    private static final String SCHOOL = "school-1";
    private static final String YEAR = "year-1";
    private static final String ENROLLMENT = "enrollment-1";
    private static final String NEW_CLASS = "class-2";
    private static final String NEW_SECTION = "section-b";

    @Mock
    private EnrollmentRepository enrollmentRepo;
    @Mock
    private AcademicYearRepository academicYearRepo;
    @Mock
    private ClassRepository classRepo;
    @Mock
    private ClassSectionRepository sectionRepo;
    @Mock
    private ClassStreamRepository streamRepo;
    @Mock
    private ClassSubjectRepository classSubjectRepo;
    @Mock
    private AttendanceRepository attendanceRepo;
    @Mock
    private BehaviourRepository behaviourRepo;
    @Mock
    private AssessmentScoreRepository assessmentScoreRepo;
    @Mock
    private StudentAssessmentRepository studentAssessmentRepo;
    @Mock
    private EnrollmentSubjectRepository enrollmentSubjectRepo;
    @Mock
    private StudentFeeRepository studentFeeRepo;
    @Mock
    private FeeDiscountRepository feeDiscountRepo;
    @Mock
    private PaymentRepository paymentRepo;
    @Mock
    private StudentLedgerEntryRepository ledgerRepo;
    @Mock
    private CreateStudentLedgerUsercase ledgerUseCase;
    @Mock
    private RecomputeStudentLedgerBalancesUseCase recomputeLedgerBalances;
    @Mock
    private RecordPaymentUseCase recordPaymentUseCase;
    @Mock
    private ProvisionStudentAssessmentsUseCase provisionStudentAssessmentsUseCase;
    @Mock
    private ApplyApplicableFeesToEnrollmentUseCase applyApplicableFeesToEnrollmentUseCase;
    @Mock
    private LogActivityUseCase logActivityUseCase;

    @InjectMocks
    private ChangeEnrollmentClassUseCase useCase;

    private Enrollment enrollment;
    private Clazz newClass;

    @BeforeEach
    void setUp() {
        var oldClass = Clazz.create("class-1", SCHOOL, null, "Class 1", Level.PRIMARY, 1);
        newClass = Clazz.create(NEW_CLASS, SCHOOL, null, "Class 2", Level.PRIMARY, 2);
        var year = AcademicYear.create(YEAR, SCHOOL, "2025/2026", LocalDate.now().minusMonths(6),
                LocalDate.now().plusMonths(6));
        var student = Student.create(SCHOOL, null, "A-001", LocalDate.now(), "Ama", "Kamara", null, null, null, null,
                null, LocalDate.now().minusYears(10), null, null, null, "Sierra Leonean", null, "Freetown", null);
        var oldSection = Section.create("section-a", SCHOOL, "A", null);
        enrollment = Enrollment.create(ENROLLMENT, SCHOOL, student, oldClass, oldSection, year, null);

        var newSection = Section.create(NEW_SECTION, SCHOOL, "B", null);

        // Defaults describe a clean, correctable enrollment; individual tests override one thing.
        lenient().when(enrollmentRepo.findByIdAndSchoolId(ENROLLMENT, SCHOOL)).thenReturn(Optional.of(enrollment));
        lenient().when(academicYearRepo.findActiveBySchool(SCHOOL)).thenReturn(Optional.of(year));
        lenient().when(classRepo.findByIdAndSchool(NEW_CLASS, SCHOOL)).thenReturn(Optional.of(newClass));
        // The link row's id ("cs-1") deliberately differs from the Section id the caller sends.
        lenient().when(sectionRepo.findByClassIdAndSchoolId(NEW_CLASS, SCHOOL))
                .thenReturn(List.of(ClassSection.create("cs-1", SCHOOL, newClass, newSection)));
        lenient().when(attendanceRepo.findByEnrollmentAndSchoolId(eq(ENROLLMENT), eq(SCHOOL), any(Pageable.class)))
                .thenReturn(Page.empty());
        lenient().when(behaviourRepo.existsByEnrollmentIdAndSchoolId(ENROLLMENT, SCHOOL)).thenReturn(false);
        lenient().when(assessmentScoreRepo.findAllByEnrollmentIdAndSchoolId(ENROLLMENT, SCHOOL)).thenReturn(List.of());
        lenient().when(feeDiscountRepo.findBySchoolAndEnrollment(eq(SCHOOL), eq(ENROLLMENT), any(Pageable.class)))
                .thenReturn(Page.empty());
        lenient().when(paymentRepo.findByStudentAndAcademicYear(any(), eq(YEAR), any(Pageable.class)))
                .thenReturn(Page.empty());
        lenient().when(ledgerRepo.findAllByStudentIdAndSchoolIdOrderByPaidAtAscCreatedAtAsc(any(), eq(SCHOOL)))
                .thenReturn(List.of());
        lenient().when(studentFeeRepo.findBySchoolAndEnrollment(eq(SCHOOL), eq(ENROLLMENT), any(Pageable.class)))
                .thenReturn(Page.empty());
        lenient().when(enrollmentSubjectRepo.findAllByEnrollmentIdAndSchoolId(ENROLLMENT, SCHOOL))
                .thenReturn(List.of());
        lenient().when(classSubjectRepo.findAllByClassIdAndSchoolId(eq(NEW_CLASS), eq(SCHOOL), any(Pageable.class)))
                .thenReturn(Page.empty());
    }

    private void assertNothingWasChanged() {
        verify(enrollmentRepo, never()).save(any());
        verify(studentFeeRepo, never()).deleteAllByEnrollmentAndSchool(anyString(), anyString());
        verify(studentAssessmentRepo, never()).deleteAllByEnrollmentIdAndSchoolId(anyString(), anyString());
        verify(ledgerRepo, never()).deleteAll(any());
        verify(ledgerUseCase, never()).createEntry(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any());
    }

    private FeeStructure fee(String id, String categoryId, String termId, String amount) {
        var fee = mock(FeeStructure.class, RETURNS_DEEP_STUBS);
        lenient().when(fee.getId()).thenReturn(id);
        lenient().when(fee.getAmount()).thenReturn(new BigDecimal(amount));
        lenient().when(fee.getCategory().getId()).thenReturn(categoryId);
        lenient().when(fee.getCategory().getName()).thenReturn(categoryId);
        lenient().when(fee.getTerm().getId()).thenReturn(termId);
        lenient().when(fee.getTerm().getName()).thenReturn(termId);
        lenient().when(fee.getTerm().getTermNumber()).thenReturn(1);
        return fee;
    }

    private Page<StudentFee> feesOf(FeeStructure... fees) {
        return new PageImpl<>(java.util.Arrays.stream(fees)
                .map(fee -> StudentFee.create(SCHOOL, enrollment, enrollment.getStudent(), fee, null)).toList());
    }

    private Payment paymentOn(FeeStructure fee, String amount) {
        return Payment.create(SCHOOL, enrollment.getStudent(), fee, new BigDecimal(amount), PaymentMethod.CASH,
                "RCT-1", null, "note", Instant.parse("2026-01-10T09:00:00Z"), "user-1");
    }

    private StudentLedgerEntry paymentLedgerEntry(Payment payment, String amount) {
        return StudentLedgerEntry.create("led-pay", SCHOOL, YEAR, enrollment.getStudent().getId(), "term-old",
                TransactionType.PAYMENT, Direction.CREDIT, new BigDecimal(amount), payment.getId(), "old text",
                payment.getPaidAt(), BigDecimal.ZERO);
    }

    @Test
    void movesTheStudentAndRebuildsWhatTheOldClassGenerated() {
        var oldFee = fee("fee-old", "tuition", "term-1", "500");
        var newFee = fee("fee-new", "tuition", "term-1", "800");
        when(studentFeeRepo.findBySchoolAndEnrollment(eq(SCHOOL), eq(ENROLLMENT), any(Pageable.class)))
                .thenReturn(feesOf(oldFee));

        var oldCharge = mock(StudentLedgerEntry.class);
        when(oldCharge.getTransactionType()).thenReturn(TransactionType.FEE_ASSINMENT);
        when(oldCharge.getReferenceId()).thenReturn("fee-old");
        var unrelated = mock(StudentLedgerEntry.class);
        when(unrelated.getTransactionType()).thenReturn(TransactionType.FEE_ASSINMENT);
        when(unrelated.getReferenceId()).thenReturn("some-other-fee");
        when(ledgerRepo.findAllByStudentIdAndSchoolIdOrderByPaidAtAscCreatedAtAsc(any(), eq(SCHOOL)))
                .thenReturn(List.of(oldCharge, unrelated));

        var result = useCase.execute(SCHOOL, ENROLLMENT, NEW_CLASS, NEW_SECTION, null);

        assertThat(enrollment.getClazz().getId()).isEqualTo(NEW_CLASS);
        assertThat(enrollment.getSection().getId()).isEqualTo(NEW_SECTION);
        assertThat(result.enrollmentId()).isEqualTo(ENROLLMENT);
        assertThat(result.requiresSubjectSelection()).isFalse();

        // Only the old class's own charge is deleted (no offsetting credit that would read as paid),
        // and that happens before the new class is provisioned and its fees charged.
        var order = inOrder(ledgerRepo, assessmentScoreRepo, studentAssessmentRepo, studentFeeRepo, enrollmentRepo,
                provisionStudentAssessmentsUseCase, applyApplicableFeesToEnrollmentUseCase, recomputeLedgerBalances);
        order.verify(ledgerRepo).deleteAll(List.of(oldCharge));
        order.verify(assessmentScoreRepo).deleteAllByEnrollmentIdAndSchoolId(ENROLLMENT, SCHOOL);
        order.verify(studentAssessmentRepo).deleteAllByEnrollmentIdAndSchoolId(ENROLLMENT, SCHOOL);
        order.verify(studentFeeRepo).deleteAllByEnrollmentAndSchool(ENROLLMENT, SCHOOL);
        order.verify(enrollmentRepo).save(enrollment);
        order.verify(provisionStudentAssessmentsUseCase).execute(enrollment);
        order.verify(applyApplicableFeesToEnrollmentUseCase).execute(enrollment);
        order.verify(recomputeLedgerBalances).recomputeForStudent(any(), eq(SCHOOL));
        verify(ledgerUseCase, never()).createEntry(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any());
    }

    @Test
    void carriesAPaymentOverToTheMatchingFeeOfTheNewClass() {
        var oldFee = fee("fee-old", "tuition", "term-1", "500");
        var newTuition = fee("fee-new-tuition", "tuition", "term-1", "800");
        var newOther = fee("fee-new-other", "sports", "term-1", "100");
        when(studentFeeRepo.findBySchoolAndEnrollment(eq(SCHOOL), eq(ENROLLMENT), any(Pageable.class)))
                .thenReturn(feesOf(oldFee), feesOf(newOther, newTuition));

        var payment = paymentOn(oldFee, "500");
        when(paymentRepo.findByStudentAndAcademicYear(any(), eq(YEAR), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment)));
        var ledgerEntry = paymentLedgerEntry(payment, "500");
        when(ledgerRepo.findAllByStudentIdAndSchoolIdOrderByPaidAtAscCreatedAtAsc(any(), eq(SCHOOL)))
                .thenReturn(List.of(ledgerEntry));

        useCase.execute(SCHOOL, ENROLLMENT, NEW_CLASS, NEW_SECTION, null);

        // Same category wins over the other fee that happens to come first in the list.
        assertThat(payment.getFee().getId()).isEqualTo("fee-new-tuition");
        assertThat(payment.getAmount()).isEqualByComparingTo("500");
        assertThat(payment.getReferenceNo()).isEqualTo("RCT-1");
        assertThat(ledgerEntry.getTermId()).isEqualTo("term-1");
        assertThat(ledgerEntry.getAmount()).isEqualByComparingTo("500");
        verify(paymentRepo).save(payment);
        verify(ledgerRepo).saveAll(List.of(ledgerEntry));
        verify(recordPaymentUseCase).processSupply(newTuition, enrollment.getStudent());
        verify(ledgerUseCase, never()).createEntry(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any());
    }

    @Test
    void splitsAPaymentAcrossFeesWhenItDoesNotFitOne() {
        var oldFee = fee("fee-old", "tuition", "term-1", "500");
        var newTuition = fee("fee-new-tuition", "tuition", "term-1", "300");
        var newOther = fee("fee-new-other", "sports", "term-1", "400");
        when(studentFeeRepo.findBySchoolAndEnrollment(eq(SCHOOL), eq(ENROLLMENT), any(Pageable.class)))
                .thenReturn(feesOf(oldFee), feesOf(newTuition, newOther));

        var payment = paymentOn(oldFee, "500");
        when(paymentRepo.findByStudentAndAcademicYear(any(), eq(YEAR), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment)));

        useCase.execute(SCHOOL, ENROLLMENT, NEW_CLASS, NEW_SECTION, null);

        assertThat(payment.getFee().getId()).isEqualTo("fee-new-tuition");
        assertThat(payment.getAmount()).isEqualByComparingTo("300");

        var saved = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepo, org.mockito.Mockito.times(2)).save(saved.capture());
        var extra = saved.getAllValues().get(1);
        assertThat(extra.getFee().getId()).isEqualTo("fee-new-other");
        assertThat(extra.getAmount()).isEqualByComparingTo("200");
        assertThat(extra.getReferenceNo()).isEqualTo("RCT-1");
        verify(ledgerUseCase).createEntry(eq(SCHOOL), eq(YEAR), any(), eq("term-1"), eq(TransactionType.PAYMENT),
                eq(Direction.CREDIT), eq(new BigDecimal("200")), eq(extra.getId()), anyString(),
                eq(payment.getPaidAt()));
    }

    @Test
    void refusesWhenTheNewClassFeesCannotAbsorbWhatWasPaid() {
        var oldFee = fee("fee-old", "tuition", "term-1", "500");
        var newTuition = fee("fee-new-tuition", "tuition", "term-1", "300");
        when(studentFeeRepo.findBySchoolAndEnrollment(eq(SCHOOL), eq(ENROLLMENT), any(Pageable.class)))
                .thenReturn(feesOf(oldFee), feesOf(newTuition));
        when(paymentRepo.findByStudentAndAcademicYear(any(), eq(YEAR), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(paymentOn(oldFee, "500"))));

        assertThatThrownBy(() -> useCase.execute(SCHOOL, ENROLLMENT, NEW_CLASS, NEW_SECTION, null))
                .isInstanceOf(RuleException.class).hasMessageContaining("more than the fees");
    }

    @Test
    void refusesWhenAttendanceIsAlreadyRecorded() {
        when(attendanceRepo.findByEnrollmentAndSchoolId(eq(ENROLLMENT), eq(SCHOOL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mock(Attendance.class))));

        assertThatThrownBy(() -> useCase.execute(SCHOOL, ENROLLMENT, NEW_CLASS, NEW_SECTION, null))
                .isInstanceOf(RuleException.class).hasMessageContaining("Attendance");
        assertNothingWasChanged();
    }

    @Test
    void refusesWhenScoresHaveBeenEntered() {
        var score = mock(AssessmentScore.class);
        when(score.getScore()).thenReturn(72);
        when(assessmentScoreRepo.findAllByEnrollmentIdAndSchoolId(ENROLLMENT, SCHOOL)).thenReturn(List.of(score));

        assertThatThrownBy(() -> useCase.execute(SCHOOL, ENROLLMENT, NEW_CLASS, NEW_SECTION, null))
                .isInstanceOf(RuleException.class).hasMessageContaining("Scores");
        assertNothingWasChanged();
    }

    @Test
    void doesNotTreatUntouchedZeroScoresAsEnteredWork() {
        var untouched = mock(AssessmentScore.class);
        when(untouched.getScore()).thenReturn(0);
        when(assessmentScoreRepo.findAllByEnrollmentIdAndSchoolId(ENROLLMENT, SCHOOL))
                .thenReturn(List.of(untouched));

        useCase.execute(SCHOOL, ENROLLMENT, NEW_CLASS, NEW_SECTION, null);

        verify(enrollmentRepo).save(enrollment);
    }

    @Test
    void refusesWhenTheStudentIsAlreadyInThatClassAndSection() {
        var sameClass = Clazz.create("class-1", SCHOOL, null, "Class 1", Level.PRIMARY, 1);
        when(classRepo.findByIdAndSchool("class-1", SCHOOL)).thenReturn(Optional.of(sameClass));
        when(sectionRepo.findByClassIdAndSchoolId("class-1", SCHOOL))
                .thenReturn(List.of(ClassSection.create("cs-0", SCHOOL, sameClass, enrollment.getSection())));

        assertThatThrownBy(() -> useCase.execute(SCHOOL, ENROLLMENT, "class-1", "section-a", null))
                .isInstanceOf(RuleException.class).hasMessageContaining("already in this class");
        assertNothingWasChanged();
    }

    @Test
    void refusesWhenAnotherEnrollmentAlreadyExistsInTheTargetClass() {
        when(enrollmentRepo
                .existsByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndSchoolIdAndStreamIdIsNull(any(),
                        eq(NEW_CLASS), eq(NEW_SECTION), eq(YEAR), eq(SCHOOL)))
                .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(SCHOOL, ENROLLMENT, NEW_CLASS, NEW_SECTION, null))
                .isInstanceOf(AlreadyExistsException.class);
        assertNothingWasChanged();
    }

    @Test
    void refusesToRewriteAnEnrollmentThatWasAlreadyPromoted() {
        enrollment.promote();

        assertThatThrownBy(() -> useCase.execute(SCHOOL, ENROLLMENT, NEW_CLASS, NEW_SECTION, null))
                .isInstanceOf(RuleException.class).hasMessageContaining("active enrollment");
        assertNothingWasChanged();
    }

    @Test
    void requiresAStreamForAnSssClass() {
        var sss = Clazz.create("class-sss", SCHOOL, null, "SSS 1", Level.SSS, 10);
        when(classRepo.findByIdAndSchool("class-sss", SCHOOL)).thenReturn(Optional.of(sss));
        when(sectionRepo.findByClassIdAndSchoolId("class-sss", SCHOOL)).thenReturn(List.of(
                ClassSection.create("cs-2", SCHOOL, sss, Section.create(NEW_SECTION, SCHOOL, "B", null))));

        assertThatThrownBy(() -> useCase.execute(SCHOOL, ENROLLMENT, "class-sss", NEW_SECTION, null))
                .isInstanceOf(RuleException.class).hasMessageContaining("Stream is required");
        assertNothingWasChanged();
    }

    @Test
    void refusesASectionThatDoesNotBelongToTheTargetClass() {
        assertThatThrownBy(() -> useCase.execute(SCHOOL, ENROLLMENT, NEW_CLASS, "section-from-another-class", null))
                .isInstanceOf(NotFoundException.class).hasMessageContaining("Section");
        assertNothingWasChanged();
    }
}
