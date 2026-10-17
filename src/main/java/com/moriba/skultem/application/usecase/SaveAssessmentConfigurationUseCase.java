package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentConfigurationDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AssessmentConfiguration;
import com.moriba.skultem.domain.repository.AssessmentConfigurationRepository;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Creates or changes a section's assessment configuration. School administrators only (the controller
// enforces the role): teachers use the configuration, they never set it. The change applies to assessments
// that open AFTER it - each open or recorded assessment froze its own copy (AssessmentStructureService).
//
// Audit: the result's toString is what the shared audit log stores as the action's details, so it carries
// who-scope, the previous configuration and the new one (who and when are on the audit row itself).
@Service
@Transactional
@RequiredArgsConstructor
public class SaveAssessmentConfigurationUseCase {

    public record Result(AssessmentConfigurationDTO current, String scope, String previous, String changedTo,
            RefreshUnstartedAssessmentsUseCase.Result applied) {
        @Override
        public String toString() {
            return "Assessment configuration changed for " + scope + " | previous: " + previous + " | new: "
                    + changedTo + (applied == null ? "" : " | " + applied);
        }
    }

    private final AssessmentTemplateRepository templateRepo;
    private final AssessmentConfigurationRepository configRepo;
    private final ListAssessmentConfigurationsUseCase listUseCase;
    private final LogActivityUseCase logActivityUseCase;
    private final RefreshUnstartedAssessmentsUseCase refreshUnstartedUseCase;

    @AuditLogAnnotation(action = "ASSESSMENT_CONFIGURATION_CHANGED")
    public Result execute(String schoolId, String userId, String templateId, AssessmentStructure structure,
            int caPercentage, int formalPercentage, CaFrequency caFrequency, int caEntries,
            java.util.List<AssessmentConfiguration.PlanEntry> plan, boolean applyToUnstarted,
            java.util.List<com.moriba.skultem.domain.vo.CaComponent> components, String formalLabel) {
        var template = templateRepo.findByIdAndSchoolId(templateId, schoolId)
                .orElseThrow(() -> new NotFoundException("Assessment template not found"));
        String scope = "template " + template.getName();

        var existing = configRepo.findByTemplate(schoolId, templateId).orElse(null);
        String previous = existing != null ? existing.summary() + " (v" + existing.getVersion() + ")"
                : "not set (single score)";

        AssessmentConfiguration saved;
        if (existing != null) {
            existing.change(structure, caPercentage, formalPercentage, caFrequency, caEntries, userId);
            saved = existing;
        } else {
            saved = AssessmentConfiguration.create(schoolId, null, structure, caPercentage, formalPercentage,
                    caFrequency, caEntries, userId).forTemplate(templateId);
        }
        saved.replacePlan(plan);
        saved.replaceComponents(components, formalLabel);
        configRepo.save(saved);

        // Assessments nobody has graded yet move onto the new setup right away (a school going live with blank
        // assessments doesn't have to wait for a new term); anything with a grade in it is never touched.
        var applied = applyToUnstarted ? refreshUnstartedUseCase.execute(schoolId, templateId) : null;

        String changedTo = saved.summary() + " (v" + saved.getVersion() + ")";
        logActivityUseCase.log(schoolId, ActivityType.SETTINGS, "Assessment configuration changed",
                scope + ": " + saved.summary(), applied == null ? "Applies to assessments that open from now on" : applied.toString(), schoolId);

        var dto = listUseCase.toDTO(saved, template.getName(), false);
        return new Result(dto, scope, previous, changedTo, applied);
    }
}
