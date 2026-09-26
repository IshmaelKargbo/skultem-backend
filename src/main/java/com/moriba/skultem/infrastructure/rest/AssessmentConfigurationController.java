package com.moriba.skultem.infrastructure.rest;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.AssessmentConfigurationDTO;
import com.moriba.skultem.application.usecase.ListAssessmentConfigurationsUseCase;
import com.moriba.skultem.application.usecase.RefreshUnstartedAssessmentsUseCase;
import com.moriba.skultem.application.usecase.SaveAssessmentConfigurationUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.SaveAssessmentConfigurationDTO;
import com.moriba.skultem.infrastructure.security.SectionNeutral;
import com.moriba.skultem.infrastructure.security.SectionScoped;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// How each management section assesses its students. Administrators (owner, proprietor, admin) configure it;
// teachers can read it, to know how to record, but never change it; parents and accountants don't need it.
@RestController
@RequestMapping("/api/v1/assessment-configuration")
@RequiredArgsConstructor
public class AssessmentConfigurationController {

    private final ListAssessmentConfigurationsUseCase listUseCase;
    private final SaveAssessmentConfigurationUseCase saveUseCase;
    private final RefreshUnstartedAssessmentsUseCase refreshUnstartedUseCase;

    // Read-only for teachers. A section-limited caller only gets their own sections' (filtered in the use case).
    @SectionNeutral
    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER')")
    public ApiResponse<List<AssessmentConfigurationDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return new ApiResponse<>("success", 200, "Assessment configuration fetched successfully",
                listUseCase.execute(school));
    }

    // Administrators only. A section-limited Admin may set their own section's; the school-wide default
    // (no sectionId) stays with whole-school staff (@sectionScope.managementSection).
    @SectionScoped
    @PutMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER') and @sectionScope.managementSection(#sectionId)")
    public ApiResponse<SaveResponse> save(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @RequestParam(required = false) String sectionId,
            @Valid @RequestBody SaveAssessmentConfigurationDTO param) {
        var plan = param.plan() == null ? java.util.List.<com.moriba.skultem.domain.model.AssessmentConfiguration.PlanEntry>of()
                : param.plan().stream().map(p -> new com.moriba.skultem.domain.model.AssessmentConfiguration.PlanEntry(
                        p.termId(), p.assessmentName(), p.usesCa(), p.caEntries())).toList();
        var res = saveUseCase.execute(school, userId, sectionId, param.structure(),
                param.caPercentage() == null ? 0 : param.caPercentage(),
                param.formalPercentage() == null ? 100 : param.formalPercentage(), param.caFrequency(),
                param.caEntries() == null ? 0 : param.caEntries(), plan,
                param.applyToUnstarted() == null || param.applyToUnstarted());
        return new ApiResponse<>("success", 200,
                res.applied() == null ? "Assessment configuration saved. It applies to assessments that open from now on."
                        : "Assessment configuration saved. " + res.applied(),
                new SaveResponse(res.current(), res.applied() == null ? null : res.applied().refreshed(),
                        res.applied() == null ? null : res.applied().skipped()));
    }

    // The saved configuration, plus - when it was applied to blank assessments straight away - how many moved and
    // how many were left alone because they already have grades.
    public record SaveResponse(AssessmentConfigurationDTO configuration, Integer refreshed, Integer skipped) {
    }

    // Moves every still-blank assessment in the section (nobody has recorded a grade in it yet) onto the
    // configuration in force now, instead of leaving it stuck with whatever it was frozen with before the
    // config changed - see RefreshUnstartedAssessmentsUseCase. Anything with even one recorded score is
    // left exactly as it is.
    @SectionScoped
    @PostMapping("/apply-to-unstarted")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER') and @sectionScope.managementSection(#sectionId)")
    public ApiResponse<RefreshUnstartedAssessmentsUseCase.Result> applyToUnstarted(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String sectionId) {
        var res = refreshUnstartedUseCase.execute(school, sectionId);
        return new ApiResponse<>("success", 200, res.toString(), res);
    }
}
