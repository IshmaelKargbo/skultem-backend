package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.FeeChargePurgeRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Deletes a fee structure the school doesn't want - one charged by mistake, or one a new academic year
 * just copied forward that isn't needed (see {@link ConfigureNextAcademicYearUseCase}).
 * If it was already charged to students, the charge is taken back off each of them: their student fee,
 * any discount on it, and the matching ledger entries go, and their running balances are recomputed.
 * Refused once anyone has paid towards it - that money has to stay on record.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteFeeStructureUseCase {

    private final FeeStructureRepository repo;
    private final FeeChargePurgeRepository chargeRepo;
    private final RecomputeStudentLedgerBalancesUseCase recomputeBalances;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "FEE_STRUCTURE_DELETED")
    public void execute(String schoolId, String feeId) {
        var fee = repo.findByIdAndSchoolId(feeId, schoolId)
                .orElseThrow(() -> new NotFoundException("Fee structure not found"));

        if (fee.isSystem()) {
            throw new RuleException("The platform fee can't be deleted");
        }

        long payments = chargeRepo.countPayments(schoolId, feeId);
        if (payments > 0) {
            throw new RuleException("This fee has " + payments + " payment(s) recorded against it, so it can't be "
                    + "deleted. Edit it instead.");
        }

        List<String> students = chargeRepo.removeCharges(schoolId, feeId);
        students.forEach(studentId -> recomputeBalances.recomputeForStudent(studentId, schoolId));

        repo.deleteById(feeId);

        logActivityUseCase.log(
                schoolId,
                ActivityType.FEES,
                "Fee structure deleted",
                fee.getCategory().getName() + " - " + fee.getAcademicYear().getName(),
                students.isEmpty() ? null : "chargesRemoved=" + students.size(),
                feeId);
    }
}
