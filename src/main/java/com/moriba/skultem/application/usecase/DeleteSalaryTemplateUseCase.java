package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.SalaryTemplateRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Deleting a template never touches a SalaryStructure built from it - templateId/templateName are
// copied, not linked (see SalaryStructure), and salary_structures.template_id is ON DELETE SET
// NULL besides.
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteSalaryTemplateUseCase {
    private final SalaryTemplateRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SALARY_TEMPLATE_DELETED")
    public void execute(String schoolId, String templateId) {
        var template = repo.findByIdAndSchoolId(templateId, schoolId)
                .orElseThrow(() -> new NotFoundException("Salary template not found"));

        repo.deleteByIdAndSchoolId(templateId, schoolId);

        logActivityUseCase.log(schoolId, ActivityType.PAYMENT, "Salary template deleted", template.getName(), null,
                template.getId());
    }
}
