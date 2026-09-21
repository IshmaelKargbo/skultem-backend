package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;

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
import com.moriba.skultem.domain.model.Stream;
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
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Corrects a student's class when they were placed in the wrong one at enrollment ("profiling").
// This is a correction, not a transfer: it is only allowed while nothing has been recorded against
// the enrollment yet (attendance, behaviour, entered scores, discounts, payments), because all of
// that hangs off the old class and can't be moved without rewriting history. Everything the old
// class generated - subject selections, provisioned assessments, fee charges - is undone (fee
// charges via an ADJUSTMENT credit so the ledger keeps a trail rather than losing rows) and then
// regenerated for the new class exactly as a fresh enrollment would.
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
    private final CreateStudentLedgerUsercase ledgerUseCase;
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

        reverseFeeCharges(schoolId, enrollment);
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

        boolean requiresSubjectSelection = classSubjectRepo
                .findAllByClassIdAndSchoolId(clazz.getId(), schoolId, Pageable.unpaged()).getContent().stream()
                .anyMatch(cs -> cs.getGroup() != null);

        logActivityUseCase.log(schoolId, ActivityType.STUDENT, "Student class corrected",
                student.getGivenNames() + " " + student.getFamilyName() + " - " + oldClassName + " to "
                        + clazz.getName(),
                "from=" + oldClassName + ";to=" + clazz.getName(), student.getId());

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

        BigDecimal paid = paymentRepo.sumPaymentsByStudentThisYear(enrollment.getStudent().getId(),
                enrollment.getAcademicYear().getId());
        if (paid != null && paid.signum() > 0) {
            throw new RuleException(
                    "This student has already made payments this year, so their class can no longer be corrected");
        }
    }

    // Credits back exactly what ApplyApplicableFeesToEnrollmentUseCase debited, so the balance
    // returns to where it was before the wrong class's fees were charged.
    private void reverseFeeCharges(String schoolId, Enrollment enrollment) {
        var student = enrollment.getStudent();
        studentFeeRepo.findBySchoolAndEnrollment(schoolId, enrollment.getId(), Pageable.unpaged()).getContent()
                .forEach(studentFee -> {
                    var fee = studentFee.getFee();
                    ledgerUseCase.createEntry(
                            schoolId,
                            enrollment.getAcademicYear().getId(),
                            student.getId(),
                            fee.getTerm().getId(),
                            TransactionType.ADJUSTMENT,
                            Direction.CREDIT,
                            fee.getAmount(),
                            fee.getId(),
                            "Reversal of " + fee.getCategory().getName() + " (" + fee.getTerm().getName()
                                    + ") - class corrected for " + student.getGivenNames() + " "
                                    + student.getFamilyName(),
                            Instant.now());
                });
    }

    public record ChangeClassResult(String enrollmentId, boolean requiresSubjectSelection) {
    }
}
