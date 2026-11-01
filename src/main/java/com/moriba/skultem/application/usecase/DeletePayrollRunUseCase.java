package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.PayrollRunRepository;
import com.moriba.skultem.domain.repository.PayslipRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Deletes a payroll run started by mistake. Drafts only: nothing has been recorded in the ledger
 * or shown to staff yet (that happens at generate/publish), so removing one leaves no trace to undo.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeletePayrollRunUseCase {
    private final PayrollRunRepository runRepo;
    private final PayslipRepository payslipRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "PAYROLL_RUN_DELETED")
    public void execute(String schoolId, String runId) {
        var run = runRepo.findByIdAndSchoolId(runId, schoolId)
                .orElseThrow(() -> new NotFoundException("Payroll run not found"));

        if (!run.isDraft()) {
            throw new BadRequestException(
                    "Only a draft payroll run can be deleted - this one has already been generated");
        }

        payslipRepo.deleteAllByPayrollRunId(run.getId());
        runRepo.deleteById(run.getId());

        logActivityUseCase.log(schoolId, ActivityType.PAYMENT, "Payroll run deleted", run.getPeriod(), null,
                run.getId());
    }
}
