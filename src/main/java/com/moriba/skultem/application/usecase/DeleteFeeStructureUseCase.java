package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Deletes a fee structure the school doesn't want - most usefully, one a new academic year just
 * copied forward that isn't needed for this year (see {@link ConfigureNextAcademicYearUseCase}).
 * Blocked once it has actually been charged to at least one student: reversing already-issued
 * {@code StudentFee}/ledger entries isn't something this does, so an in-use fee structure has to be
 * left alone rather than deleted out from under students who are already carrying that charge.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteFeeStructureUseCase {

    private final FeeStructureRepository repo;
    private final StudentFeeRepository studentFeeRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "FEE_STRUCTURE_DELETED")
    public void execute(String schoolId, String feeId) {
        var fee = repo.findByIdAndSchoolId(feeId, schoolId)
                .orElseThrow(() -> new NotFoundException("Fee structure not found"));

        if (fee.isSystem()) {
            throw new RuleException("The platform fee can't be deleted");
        }

        long assignedCount = studentFeeRepo.countByFeeAndSchool(feeId, schoolId);
        if (assignedCount > 0) {
            throw new RuleException(
                    "This fee structure is already charged to " + assignedCount
                            + " student(s) and can't be deleted. Edit it instead, or remove it before it's applied to anyone.");
        }

        repo.deleteById(feeId);

        logActivityUseCase.log(
                schoolId,
                ActivityType.FEES,
                "Fee structure deleted",
                fee.getCategory().getName() + " - " + fee.getAcademicYear().getName(),
                null,
                feeId);
    }
}
