package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PayrollRunDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PayrollRunMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.PayrollRunRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PublishPayrollRunUseCase {
    private final PayrollRunRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "PAYROLL_RUN_PUBLISHED")
    public PayrollRunDTO execute(String schoolId, String runId) {
        var run = repo.findByIdAndSchoolId(runId, schoolId)
                .orElseThrow(() -> new NotFoundException("Payroll run not found"));

        run.publish();
        repo.save(run);

        logActivityUseCase.log(schoolId, ActivityType.PAYMENT, "Payslips published", run.getPeriod(), null,
                run.getId());

        return PayrollRunMapper.toDTO(run);
    }
}
