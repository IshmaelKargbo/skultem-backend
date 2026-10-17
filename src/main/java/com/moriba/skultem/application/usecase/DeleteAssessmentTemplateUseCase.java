package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Removes a template together with its assessments and its scoring setup. Only a template nothing depends on can go:
// no class assessed with it, and no test ever opened from it - otherwise grades and report cards would lose their
// meaning.
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteAssessmentTemplateUseCase {

    private final AssessmentTemplateRepository templateRepo;
    private final AssessmentRepository assessmentRepo;
    private final ClassRepository classRepo;
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;

    @AuditLogAnnotation(action = "ASSESSMENT_TEMPLATE_DELETED")
    public String execute(String schoolId, String templateId) {
        var template = templateRepo.findByIdAndSchoolId(templateId, schoolId)
                .orElseThrow(() -> new NotFoundException("Assessment template not found"));

        long classes = classRepo.countByTemplate(templateId);
        if (classes > 0) {
            throw new RuleException(template.getName() + " is used by " + classes + " class"
                    + (classes == 1 ? "" : "es") + ". Move them to another template first.");
        }
        if (cycleRepo.countByTemplate(templateId) > 0) {
            throw new RuleException(template.getName() + " has already been used to record assessments, so it "
                    + "can't be deleted.");
        }

        assessmentRepo.deleteAllByTemplateIdAndSchoolId(templateId, schoolId);
        templateRepo.deleteByIdAndSchoolId(templateId, schoolId);
        return template.getName();
    }
}
