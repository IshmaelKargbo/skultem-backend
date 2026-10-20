package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.Student.EnrollmentType;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Corrects a student's enrollment type (new, transfer, re-enrolment or existing) and brings the student's fees in the
 * current academic year in line with it.
 * <p>
 * Fee structures can be marked for new students only or old students only (see {@link FeeStructure}), and which group a
 * student is in comes from the enrollment type ({@link EnrollmentType#isNewAdmission()}). So when a change moves the
 * student from one group to the other, the fees they now qualify for are charged and the ones they no longer qualify for
 * are taken off. Changing between types in the same group (NEW to TRANSFER, say) leaves the fees untouched.
 * <p>
 * A fee that would be taken off but already has a payment or a discount against it is never removed behind the
 * school's back: the whole change is refused and says which fees need sorting out first.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ChangeStudentEnrollmentTypeUseCase {

    private final StudentRepository studentRepo;
    private final AcademicYearRepository academicYearRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final StudentFeeRepository studentFeeRepo;
    private final PaymentRepository paymentRepo;
    private final FeeDiscountRepository feeDiscountRepo;
    private final StudentLedgerEntryRepository ledgerRepo;
    private final RecomputeStudentLedgerBalancesUseCase recomputeLedgerBalances;
    private final ApplyApplicableFeesToEnrollmentUseCase applyApplicableFees;
    private final LogActivityUseCase logActivityUseCase;

    public record Result(String enrollmentType, List<String> feesAdded, List<String> feesRemoved) {
    }

    @AuditLogAnnotation(action = "STUDENT_ENROLLMENT_TYPE_CHANGED")
    public Result execute(String schoolId, String studentId, EnrollmentType newType) {
        Student student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));

        EnrollmentType oldType = student.getEnrollmentType();
        if (oldType == newType) {
            throw new RuleException("The student is already marked as " + label(newType));
        }

        boolean changesGroup = oldType == null || oldType.isNewAdmission() != newType.isNewAdmission();

        // Fees live on the enrollment in the active year; a locked year is history and is left as it was.
        Enrollment enrollment = null;
        if (changesGroup) {
            var year = academicYearRepo.findActiveBySchool(schoolId).orElse(null);
            if (year != null && !year.isLocked()) {
                enrollment = enrollmentRepo.findByStudentAndAcademicYearAndSchoolId(studentId, year.getId(), schoolId)
                        .filter(e -> e.getStatus() == Enrollment.Status.ACTIVE).orElse(null);
            }
        }

        List<StudentFee> toRemove = new ArrayList<>();
        Set<String> feeIdsBefore = Set.of();
        if (enrollment != null) {
            var current = studentFeeRepo.findBySchoolAndEnrollment(schoolId, enrollment.getId(), Pageable.unpaged())
                    .getContent();
            feeIdsBefore = current.stream().map(sf -> sf.getFee().getId()).collect(Collectors.toSet());

            for (StudentFee sf : current) {
                FeeStructure fee = sf.getFee();
                boolean stillApplies = !(fee.isNewStudentsOnly() && !newType.isNewAdmission())
                        && !(fee.isOldStudentsOnly() && newType.isNewAdmission());
                if (!stillApplies) {
                    toRemove.add(sf);
                }
            }

            // Nothing is changed until every fee that would be removed is known to be safe to remove.
            List<String> blocked = new ArrayList<>();
            for (StudentFee sf : toRemove) {
                boolean paid = paymentRepo.sumPaymentsByStudentAndFee(studentId, sf.getFee().getId()).signum() > 0;
                boolean discounted = sf.getDiscount() != null || !feeDiscountRepo
                        .findBySchoolAndStudentIdAndFeeId(schoolId, studentId, sf.getFee().getId()).isEmpty();
                if (paid || discounted) {
                    blocked.add(feeLabel(sf.getFee()) + (paid ? " (has payments)" : " (has a discount)"));
                }
            }
            if (!blocked.isEmpty()) {
                throw new RuleException("This change would take " + String.join(", ", blocked)
                        + " off the student, but money or a discount is already recorded against it. "
                        + "Reverse that first, then change the type.");
            }
        }

        student.changeEnrollmentType(newType);
        studentRepo.save(student);

        List<String> removed = new ArrayList<>();
        List<String> added = new ArrayList<>();

        if (enrollment != null) {
            if (!toRemove.isEmpty()) {
                Set<String> removeIds = toRemove.stream().map(sf -> sf.getFee().getId()).collect(Collectors.toSet());
                // The charges are deleted outright rather than offset by a credit, which would read as a payment.
                ledgerRepo.deleteAll(ledgerRepo
                        .findAllByStudentIdAndSchoolIdOrderByPaidAtAscCreatedAtAsc(studentId, schoolId).stream()
                        .filter(entry -> entry.getTransactionType() == TransactionType.FEE_ASSINMENT
                                && entry.getReferenceId() != null && removeIds.contains(entry.getReferenceId()))
                        .toList());
                for (StudentFee sf : toRemove) {
                    studentFeeRepo.deleteByEnrollmentAndFeeAndSchool(enrollment.getId(), sf.getFee().getId(), schoolId);
                    removed.add(feeLabel(sf.getFee()));
                }
            }

            // Reloaded so the enrollment sees the student's new type, then charged for every fee it now qualifies for.
            Enrollment fresh = enrollmentRepo.findByIdAndSchoolId(enrollment.getId(), schoolId).orElseThrow();
            applyApplicableFees.execute(fresh);

            Set<String> before = feeIdsBefore;
            studentFeeRepo.findBySchoolAndEnrollment(schoolId, enrollment.getId(), Pageable.unpaged()).getContent()
                    .stream().map(StudentFee::getFee).filter(fee -> !before.contains(fee.getId()))
                    .forEach(fee -> added.add(feeLabel(fee)));

            recomputeLedgerBalances.recomputeForStudent(studentId, schoolId);
        }

        logActivityUseCase.log(schoolId, ActivityType.STUDENT, "Enrollment type changed",
                student.getName() + " - " + label(oldType) + " to " + label(newType),
                "from=" + oldType + ";to=" + newType + ";feesAdded=" + added.size() + ";feesRemoved=" + removed.size(),
                student.getId());

        return new Result(newType.name(), added, removed);
    }

    private static String feeLabel(FeeStructure fee) {
        return fee.getCategory().getName() + " (" + fee.getTerm().getName() + ")";
    }

    private static String label(EnrollmentType type) {
        if (type == null) {
            return "unknown";
        }
        return switch (type) {
            case NEW -> "a new student";
            case TRANSFER -> "a transfer";
            case RE_ENROLLMENT -> "a re-enrolment";
            case EXISTING -> "an existing student";
        };
    }
}
