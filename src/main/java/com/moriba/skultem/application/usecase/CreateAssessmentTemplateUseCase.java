package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentTemplateDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.AssessmentTemplateMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AssessmentTemplate;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateAssessmentTemplateUseCase {
    private final AssessmentTemplateRepository templateRepo;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "ASSESSMENT_TEMPLATE_CREATED")
    public AssessmentTemplateDTO execute(String schoolId, String name, int passMark, String description) {
        var cleanName = name == null ? "" : name.trim();
        if (cleanName.isBlank()) {
            throw new RuleException("Template name is required");
        }

        var id = rg.generate("ASSESSMENT_TEMPLATE", "AST");
        var template = AssessmentTemplate.create(id, schoolId, cleanName, description.trim(), passMark);

        templateRepo.save(template);

        logActivityUseCase.log(
                schoolId,
                ActivityType.GRADE,
                "Assessment template created",
                template.getName(),
                null,
                template.getId());

        return AssessmentTemplateMapper.toDTO(template);
    }
}
