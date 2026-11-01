package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PayrollRunDTO;
import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.mapper.PayrollRunMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.PayrollRun;
import com.moriba.skultem.domain.model.Payslip;
import com.moriba.skultem.domain.repository.PayrollRunRepository;
import com.moriba.skultem.domain.repository.PayslipRepository;
import com.moriba.skultem.domain.repository.SalaryStructureRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Starting a run snapshots every teacher who currently has a salary structure into a payslip -
// see Payslip.fromSalaryStructure. A teacher hired after the run is created just isn't in it;
// they'll be included the next time a run is started.
@Service
@Transactional
@RequiredArgsConstructor
public class CreatePayrollRunUseCase {
    private final PayrollRunRepository runRepo;
    private final PayslipRepository payslipRepo;
    private final SalaryStructureRepository salaryRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "PAYROLL_RUN_CREATED")
    public PayrollRunDTO execute(String schoolId, String period, LocalDate payDate) {
        // A period already generated/published has been paid (and recorded in the ledger) -
        // running it again would pay everyone twice.
        if (runRepo.existsFinalizedForPeriod(schoolId, period)) {
            throw new BadRequestException("Payroll for " + period.trim()
                    + " has already been generated - open that run instead of starting a new one");
        }

        // Only active staff - someone deactivated or removed keeps their salary record, but
        // shouldn't land on a new run.
        var structures = salaryRepo.findAllBySchoolId(schoolId).stream()
                .filter(s -> s.getTeacher() != null
                        && s.getTeacher().getStatus() == com.moriba.skultem.domain.model.Teacher.Status.ACTIVE)
                .toList();

        if (structures.isEmpty()) {
            throw new BadRequestException(
                    "No active staff with a salary set up yet - add at least one before running payroll");
        }

        var run = PayrollRun.create(UUID.randomUUID().toString(), schoolId, period.trim(), payDate);
        runRepo.save(run);

        var payslips = structures.stream()
                .map(structure -> Payslip.fromSalaryStructure(UUID.randomUUID().toString(), run, structure))
                .toList();

        payslipRepo.saveAll(payslips);

        logActivityUseCase.log(
                schoolId,
                ActivityType.PAYMENT,
                "Payroll run created",
                period + " (" + payslips.size() + " employees)",
                null,
                run.getId());

        return PayrollRunMapper.toDTO(run);
    }
}
