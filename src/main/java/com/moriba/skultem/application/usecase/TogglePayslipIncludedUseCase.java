package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PayslipDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PayslipMapper;
import com.moriba.skultem.domain.repository.PayrollRunRepository;
import com.moriba.skultem.domain.repository.PayslipRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class TogglePayslipIncludedUseCase {
    private final PayslipRepository payslipRepo;
    private final PayrollRunRepository runRepo;

    public PayslipDTO execute(String schoolId, String runId, String payslipId, boolean included) {
        var run = runRepo.findByIdAndSchoolId(runId, schoolId)
                .orElseThrow(() -> new NotFoundException("Payroll run not found"));

        var payslip = payslipRepo.findByIdAndSchoolId(payslipId, schoolId)
                .orElseThrow(() -> new NotFoundException("Payslip not found"));

        payslip.setIncluded(included, run);
        payslipRepo.save(payslip);

        return PayslipMapper.toDTO(payslip, run);
    }
}
