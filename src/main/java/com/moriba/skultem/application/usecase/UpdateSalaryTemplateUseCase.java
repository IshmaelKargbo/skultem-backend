package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SalaryTemplateDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.PayComponentItemMapper;
import com.moriba.skultem.application.mapper.SalaryTemplateMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.SalaryTemplateRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.infrastructure.rest.dto.PayComponentItemDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Editing a template only affects structures built from it going forward - see SalaryStructure's
// templateId/templateName, copied in once at SetSalaryStructureUseCase time, never kept linked.
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateSalaryTemplateUseCase {
    private final SalaryTemplateRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SALARY_TEMPLATE_UPDATED")
    public SalaryTemplateDTO execute(String schoolId, String templateId, String name, BigDecimal basicSalary,
            List<PayComponentItemDTO> allowances, List<PayComponentItemDTO> deductions) {

        var template = repo.findByIdAndSchoolId(templateId, schoolId)
                .orElseThrow(() -> new NotFoundException("Salary template not found"));

        var cleanName = name == null ? "" : name.trim();
        if (cleanName.isBlank()) {
            throw new RuleException("Template name is required");
        }

        template.update(cleanName, basicSalary, PayComponentItemMapper.toDomain(allowances),
                PayComponentItemMapper.toDomain(deductions));

        repo.save(template);

        logActivityUseCase.log(schoolId, ActivityType.PAYMENT, "Salary template updated", template.getName(), null,
                template.getId());

        return SalaryTemplateMapper.toDTO(template);
    }
}
