package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SalaryTemplateDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.PayComponentItemMapper;
import com.moriba.skultem.application.mapper.SalaryTemplateMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.SalaryTemplate;
import com.moriba.skultem.domain.repository.SalaryTemplateRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.infrastructure.rest.dto.PayComponentItemDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateSalaryTemplateUseCase {
    private final SalaryTemplateRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SALARY_TEMPLATE_CREATED")
    public SalaryTemplateDTO execute(String schoolId, String name, BigDecimal basicSalary,
            List<PayComponentItemDTO> allowances, List<PayComponentItemDTO> deductions) {

        var cleanName = name == null ? "" : name.trim();
        if (cleanName.isBlank()) {
            throw new RuleException("Template name is required");
        }

        var template = SalaryTemplate.create(UUID.randomUUID().toString(), schoolId, cleanName, basicSalary,
                PayComponentItemMapper.toDomain(allowances), PayComponentItemMapper.toDomain(deductions));

        repo.save(template);

        logActivityUseCase.log(schoolId, ActivityType.PAYMENT, "Salary template created", template.getName(), null,
                template.getId());

        return SalaryTemplateMapper.toDTO(template);
    }
}
