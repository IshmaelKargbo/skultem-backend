package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.utils.Generate;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Charges an enrollment for every fee structure already configured for its academic year/class.
 * <p>
 * A fee structure only reaches the students who were enrolled at the moment it was created
 * ({@link CreateFeeStructureUseCase} loops over existing enrollments once, then stops) - so anyone
 * who joins that class afterwards needs this run against their new enrollment to pick up the same
 * charges: a new admission ({@link CreateStudentUseCase}), or a student promoted/repeating into it
 * ({@link ApprovePromotionRequestUseCase}).
 * <p>
 * If the school hasn't set up the new year's fee structures yet by the time a student lands here,
 * this is simply a no-op for them - creating the fee structure afterwards already loops over every
 * enrollment that exists at that point, including theirs, so nothing is lost either way.
 */
@Service
// Callers (e.g. ApprovePromotionRequestUseCase) treat RuleException/NotFoundException from here as
// a per-student, best-effort failure and catch it to keep going - but since this method joins the
// caller's transaction (default REQUIRED propagation), Spring would otherwise mark that shared
// transaction rollback-only the moment either exception leaves this method, dooming the whole batch
// with an UnexpectedRollbackException at commit even though the caller "handled" it.
@Transactional(dontRollbackOn = { RuleException.class, NotFoundException.class })
@RequiredArgsConstructor
public class ApplyApplicableFeesToEnrollmentUseCase {

    private final FeeStructureRepository feeStructureRepo;
    private final StudentFeeRepository studentFeeRepo;
    private final CreateStudentLedgerUsercase createStudentLedgerUsercase;
    private final LogActivityUseCase logActivityUseCase;

    public void execute(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        String schoolId = enrollment.getSchoolId();

        List<FeeStructure> fees = feeStructureRepo.findApplicableFees(schoolId, enrollment.getAcademicYear().getId(),
                enrollment.getClazz().getId());

        int assignedCount = 0;
        BigDecimal totalAssignedAmount = BigDecimal.ZERO;

        for (FeeStructure fee : fees) {
            if (studentFeeRepo.existsBySchoolAndEnrollmentAndStudentAndFee(schoolId, enrollment.getId(),
                    student.getId(), fee.getId())) {
                continue;
            }

            StudentFee studentFee = StudentFee.create(schoolId, enrollment, student, fee, null);
            studentFeeRepo.save(studentFee);

            String description = Generate.generateLedgerDescription(
                    TransactionType.FEE_ASSINMENT,
                    fee.getTerm().getName(),
                    fee.getCategory().getName(),
                    student.getGivenNames(),
                    student.getFamilyName(),
                    student.getAdmissionNumber(),
                    fee.getAmount());

            createStudentLedgerUsercase.createEntry(
                    schoolId,
                    enrollment.getAcademicYear().getId(),
                    student.getId(),
                    fee.getTerm().getId(),
                    TransactionType.FEE_ASSINMENT,
                    Direction.DEBIT,
                    fee.getAmount(),
                    fee.getId(),
                    description,
                    Instant.now());

            assignedCount += 1;
            totalAssignedAmount = totalAssignedAmount.add(fee.getAmount());
        }

        if (assignedCount > 0) {
            String meta = "assignedCount=" + assignedCount + ";totalAmount=" + totalAssignedAmount;
            logActivityUseCase.log(
                    schoolId,
                    ActivityType.FEES,
                    "Fees assigned to student",
                    student.getGivenNames() + " " + student.getFamilyName(),
                    meta,
                    student.getId());
        }
    }
}
