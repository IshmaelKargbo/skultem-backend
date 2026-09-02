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
        var structures = salaryRepo.findAllBySchoolId(schoolId);

        if (structures.isEmpty()) {
            throw new BadRequestException("No salary structures set up yet - add at least one before running payroll");
        }

        var run = PayrollRun.create(UUID.randomUUID().toString(), schoolId, period, payDate);
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
