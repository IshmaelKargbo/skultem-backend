package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PayrollRunDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PayrollRunMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Payslip;
import com.moriba.skultem.domain.model.Transaction.Direction;
import com.moriba.skultem.domain.model.Transaction.ReferenceType;
import com.moriba.skultem.domain.model.Transaction.TransactionType;
import com.moriba.skultem.domain.repository.PayrollRunRepository;
import com.moriba.skultem.domain.repository.PayslipRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GeneratePayrollRunUseCase {
    private final PayrollRunRepository repo;
    private final PayslipRepository payslipRepo;
    private final LogActivityUseCase logActivityUseCase;
    private final CreateTransactionUsercase createTransactionUsercase;

    @AuditLogAnnotation(action = "PAYROLL_RUN_GENERATED")
    public PayrollRunDTO execute(String schoolId, String runId) {
        var run = repo.findByIdAndSchoolId(runId, schoolId)
                .orElseThrow(() -> new NotFoundException("Payroll run not found"));

        run.generate();
        repo.save(run);

        // Generating locks the numbers in - this is the point payroll becomes a real financial
        // commitment, same footing as Expense (see CreateExpenseUseCase). Net pay, not gross: the
        // deductions withheld aren't cash the school has paid out yet, they've just been set aside.
        BigDecimal netTotal = payslipRepo.findAllByPayrollRunId(run.getId()).stream()
                .filter(Payslip::isIncluded)
                .map(Payslip::netSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (netTotal.compareTo(BigDecimal.ZERO) > 0) {
            createTransactionUsercase.createEntry(schoolId, TransactionType.PAYROLL, Direction.DEBIT, netTotal,
                    run.getId(), ReferenceType.PAYROLL);
        }

        logActivityUseCase.log(schoolId, ActivityType.PAYMENT, "Payroll run generated", run.getPeriod(), null,
                run.getId());

        return PayrollRunMapper.toDTO(run);
    }
}
