package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.ClassSection;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.Payment;
import com.moriba.skultem.domain.model.Stream;
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
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.utils.Generate;
import com.moriba.skultem.utils.MoneyUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Corrects a student's class when they were placed in the wrong one at enrollment ("profiling").
// This is a correction, not a transfer: it is only allowed while nothing has been recorded against
// the enrollment yet (attendance, behaviour, entered scores, discounts), because all of that hangs
// off the old class and can't be moved without rewriting history. Everything the old class
// generated - subject selections, provisioned assessments, fee charges and their ledger entries -
// is deleted and regenerated for the new class exactly as a fresh enrollment would. Money the
// student already paid is kept, not refunded: each payment is re-applied to the new class's fees
// (same receipt, method and date) and its ledger entry updated to match. The school's cashbook
// (Transaction) is deliberately left alone - the cash was received either way.
@Service
@Transactional
@RequiredArgsConstructor
public class ChangeEnrollmentClassUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChangeEnrollmentClassUseCase.class);

    private final EnrollmentRepository enrollmentRepo;
    private final AcademicYearRepository academicYearRepo;
    private final ClassRepository classRepo;
    private final ClassSectionRepository sectionRepo;
    private final ClassStreamRepository streamRepo;
    private final ClassSubjectRepository classSubjectRepo;
    private final AttendanceRepository attendanceRepo;
    private final BehaviourRepository behaviourRepo;
    private final AssessmentScoreRepository assessmentScoreRepo;
    private final StudentAssessmentRepository studentAssessmentRepo;
    private final EnrollmentSubjectRepository enrollmentSubjectRepo;
    private final StudentFeeRepository studentFeeRepo;
    private final FeeDiscountRepository feeDiscountRepo;
    private final PaymentRepository paymentRepo;
    private final StudentLedgerEntryRepository ledgerRepo;
    private final CreateStudentLedgerUsercase ledgerUseCase;
    private final RecomputeStudentLedgerBalancesUseCase recomputeLedgerBalances;
    private final RecordPaymentUseCase recordPaymentUseCase;
    private final ProvisionStudentAssessmentsUseCase provisionStudentAssessmentsUseCase;
    private final ApplyApplicableFeesToEnrollmentUseCase applyApplicableFeesToEnrollmentUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "ENROLLMENT_CLASS_CHANGED")
    public ChangeClassResult execute(String schoolId, String enrollmentId, String classId, String sectionId,
            String streamId) {

        Enrollment enrollment = enrollmentRepo.findByIdAndSchoolId(enrollmentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found"));

        var activeYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Academic year not found"));

        if (!activeYear.getId().equals(enrollment.getAcademicYear().getId())) {
            throw new RuleException("Only an enrollment in the active academic year can be corrected");
        }
        if (activeYear.isLocked()) {
            throw new RuleException("Academic year is locked");
        }
        if (enrollment.getStatus() != Enrollment.Status.ACTIVE) {
            throw new RuleException("Only an active enrollment can be moved to another class");
        }

        var clazz = classRepo.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));
        // sectionId is the Section's own id (what a class session exposes), not the ClassSection
        // link's id that findByIdAndClassIdAndSchoolId expects - so match within the class's sections.
        var section = sectionRepo.findByClassIdAndSchoolId(clazz.getId(), schoolId).stream()
                .map(ClassSection::getSection)
                .filter(candidate -> candidate.getId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Section not found"));

        Stream stream = null;
        if (clazz.getLevel() == Level.SSS) {
            if (streamId == null || streamId.isBlank()) {
                throw new RuleException("Stream is required for SSS class");
            }
            stream = streamRepo.findByClassIdAndSchoolIdAndStreamId(clazz.getId(), schoolId, streamId)
                    .orElseThrow(() -> new RuleException("Stream does not belong to this class")).getStream();
        } else if (streamId != null && !streamId.isBlank()) {
            throw new RuleException("Stream is not allowed for this class");
        }

        boolean samePlacement = enrollment.getClazz().getId().equals(clazz.getId())
                && enrollment.getSection().getId().equals(section.getId())
                && java.util.Objects.equals(
                        enrollment.getStream() != null ? enrollment.getStream().getId() : null,
                        stream != null ? stream.getId() : null);
        if (samePlacement) {
            throw new RuleException("The student is already in this class");
        }

        var student = enrollment.getStudent();
        boolean alreadyThere = stream != null
                ? enrollmentRepo.existsByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndStreamIdAndSchoolId(
                        student.getId(), clazz.getId(), section.getId(), activeYear.getId(), stream.getId(), schoolId)
                : enrollmentRepo.existsByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndSchoolIdAndStreamIdIsNull(
                        student.getId(), clazz.getId(), section.getId(), activeYear.getId(), schoolId);
        if (alreadyThere) {
            throw new AlreadyExistsException("The student is already enrolled in this class");
        }

        assertNothingRecorded(schoolId, enrollment);

        String oldClassName = enrollment.getClazz().getName();

        var oldFees = studentFeeRepo.findBySchoolAndEnrollment(schoolId, enrollment.getId(), Pageable.unpaged())
                .getContent().stream().map(StudentFee::getFee).toList();
        Set<String> oldFeeIds = oldFees.stream().map(FeeStructure::getId).collect(Collectors.toSet());

        // Oldest first, so the earliest money is the first to be matched to the new fees.
        List<Payment> paidOnOldFees = paymentRepo
                .findByStudentAndAcademicYear(student.getId(), activeYear.getId(), Pageable.unpaged()).getContent()
                .stream()
                .filter(payment -> oldFeeIds.contains(payment.getFee().getId()))
                .sorted(Comparator.comparing(Payment::getPaidAt))
                .toList();

        // The old charges are deleted outright (not offset by a credit, which would read as paid).
        var ledgerEntries = ledgerRepo.findAllByStudentIdAndSchoolIdOrderByPaidAtAscCreatedAtAsc(student.getId(),
                schoolId);
        ledgerRepo.deleteAll(ledgerEntries.stream()
                .filter(entry -> entry.getTransactionType() == TransactionType.FEE_ASSINMENT
                        && entry.getReferenceId() != null && oldFeeIds.contains(entry.getReferenceId()))
                .toList());

        assessmentScoreRepo.deleteAllByEnrollmentIdAndSchoolId(enrollment.getId(), schoolId);
        studentAssessmentRepo.deleteAllByEnrollmentIdAndSchoolId(enrollment.getId(), schoolId);
        studentFeeRepo.deleteAllByEnrollmentAndSchool(enrollment.getId(), schoolId);
        enrollmentSubjectRepo.findAllByEnrollmentIdAndSchoolId(enrollment.getId(), schoolId)
                .forEach(enrollmentSubjectRepo::delete);

        enrollment.changePlacement(clazz, section, stream);
        enrollmentRepo.save(enrollment);

        // Same as a fresh enrollment: a missing class session/template surfaces as an error and
        // rolls the whole move back, so the student is never left in the new class with no
        // assessments; fees stay best-effort because a school may not have set them up yet.
        provisionStudentAssessmentsUseCase.execute(enrollment);
        try {
            applyApplicableFeesToEnrollmentUseCase.execute(enrollment);
        } catch (RuleException | NotFoundException e) {
            log.warn("Could not apply fee structures for student {}: {}", student.getId(), e.getMessage());
        }

        carryPaymentsOver(schoolId, enrollment, paidOnOldFees, ledgerEntries);
        recomputeLedgerBalances.recomputeForStudent(student.getId(), schoolId);

        boolean requiresSubjectSelection = classSubjectRepo
                .findAllByClassIdAndSchoolId(clazz.getId(), schoolId, Pageable.unpaged()).getContent().stream()
                .anyMatch(cs -> cs.getGroup() != null);

        logActivityUseCase.log(schoolId, ActivityType.STUDENT, "Student class corrected",
                student.getGivenNames() + " " + student.getFamilyName() + " - " + oldClassName + " to "
                        + clazz.getName(),
                "from=" + oldClassName + ";to=" + clazz.getName() + ";paymentsMoved=" + paidOnOldFees.size(),
                student.getId());

        return new ChangeClassResult(enrollment.getId(), requiresSubjectSelection);
    }

    private void assertNothingRecorded(String schoolId, Enrollment enrollment) {
        String enrollmentId = enrollment.getId();

        if (attendanceRepo.findByEnrollmentAndSchoolId(enrollmentId, schoolId, Pageable.ofSize(1)).hasContent()) {
            throw new RuleException("Attendance has already been recorded for this student in the current class");
        }
        if (behaviourRepo.existsByEnrollmentIdAndSchoolId(enrollmentId, schoolId)) {
            throw new RuleException("Behaviour records already exist for this student in the current class");
        }
        if (assessmentScoreRepo.findAllByEnrollmentIdAndSchoolId(enrollmentId, schoolId).stream()
                .anyMatch(s -> s.getScore() != null && s.getScore() > 0)) {
            throw new RuleException("Scores have already been entered for this student in the current class");
        }
        if (feeDiscountRepo.findBySchoolAndEnrollment(schoolId, enrollmentId, Pageable.ofSize(1)).hasContent()) {
            throw new RuleException("A fee discount has already been applied to this student");
        }
    }

    // Re-applies the money already paid to the new class's fees: matching category and term first,
    // then anything left over to the remaining fees in term order. A payment that doesn't fit one
    // fee is split (extra rows share its receipt number, which the model already allows). If the
    // new class's fees can't absorb everything paid, the whole change is refused rather than
    // silently losing part of what the parent paid.
    private void carryPaymentsOver(String schoolId, Enrollment enrollment, List<Payment> payments,
            List<StudentLedgerEntry> ledgerEntries) {
        if (payments.isEmpty()) {
            return;
        }

        var student = enrollment.getStudent();
        String yearId = enrollment.getAcademicYear().getId();

        List<FeeStructure> newFees = studentFeeRepo
                .findBySchoolAndEnrollment(schoolId, enrollment.getId(), Pageable.unpaged()).getContent().stream()
                .map(StudentFee::getFee)
                .sorted(Comparator.comparingInt((FeeStructure fee) -> fee.getTerm().getTermNumber())
                        .thenComparing(fee -> fee.getCategory().getName()))
                .toList();

        Map<String, BigDecimal> room = new HashMap<>();
        newFees.forEach(fee -> room.put(fee.getId(), fee.getAmount()));

        Map<String, StudentLedgerEntry> ledgerByPayment = ledgerEntries.stream()
                .filter(entry -> entry.getTransactionType() == TransactionType.PAYMENT
                        && entry.getReferenceId() != null)
                .collect(Collectors.toMap(StudentLedgerEntry::getReferenceId, entry -> entry, (a, b) -> a));

        Map<String, FeeStructure> feesPaidInto = new LinkedHashMap<>();

        for (Payment payment : payments) {
            BigDecimal left = payment.getAmount();
            boolean firstPortion = true;

            for (FeeStructure fee : bestMatchFirst(newFees, payment.getFee())) {
                if (left.signum() <= 0) {
                    break;
                }
                BigDecimal available = room.get(fee.getId());
                if (available.signum() <= 0) {
                    continue;
                }

                BigDecimal portion = left.min(available);
                room.put(fee.getId(), available.subtract(portion));
                left = left.subtract(portion);
                feesPaidInto.put(fee.getId(), fee);

                String description = Generate.generateLedgerDescription(TransactionType.PAYMENT,
                        fee.getTerm().getName(), fee.getCategory().getName(), student.getGivenNames(),
                        student.getFamilyName(), student.getAdmissionNumber(), portion);

                if (firstPortion) {
                    firstPortion = false;
                    payment.reassign(fee, portion);
                    paymentRepo.save(payment);

                    var entry = ledgerByPayment.get(payment.getId());
                    if (entry != null) {
                        entry.reassignPayment(fee.getTerm().getId(), portion, description);
                        ledgerRepo.saveAll(List.of(entry));
                    }
                } else {
                    var extra = Payment.create(schoolId, student, fee, portion, payment.getMethod(),
                            payment.getReferenceNo(), payment.getExternalReference(), payment.getNote(),
                            payment.getPaidAt(), payment.getRecordedByUserId());
                    paymentRepo.save(extra);
                    ledgerUseCase.createEntry(schoolId, yearId, student.getId(), fee.getTerm().getId(),
                            TransactionType.PAYMENT, Direction.CREDIT, portion, extra.getId(), description,
                            payment.getPaidAt());
                }
            }

            if (left.signum() > 0) {
                throw new RuleException("This student has paid " + MoneyUtil.format(left)
                        + " more than the fees of the new class cover. Set up the new class's fees first, then "
                        + "change the class again.");
            }
        }

        // A fee that is now fully paid issues its supplies (uniform etc.), same as paying it directly;
        // anything already issued for the old fee is skipped by processSupply itself.
        feesPaidInto.values().forEach(fee -> recordPaymentUseCase.processSupply(fee, student));
    }

    private List<FeeStructure> bestMatchFirst(List<FeeStructure> fees, FeeStructure paidFee) {
        return fees.stream().sorted(Comparator.comparingInt(fee -> {
            boolean sameCategory = fee.getCategory().getId().equals(paidFee.getCategory().getId());
            boolean sameTerm = fee.getTerm().getId().equals(paidFee.getTerm().getId());
            return sameCategory && sameTerm ? 0 : sameCategory ? 1 : 2;
        })).toList();
    }

    public record ChangeClassResult(String enrollmentId, boolean requiresSubjectSelection) {
    }
}
