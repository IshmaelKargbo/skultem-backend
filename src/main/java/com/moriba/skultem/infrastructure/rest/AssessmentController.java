package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.AssessmentApprovalRequestDTO;
import com.moriba.skultem.application.dto.AssessmentCycleDTO;
import com.moriba.skultem.application.dto.AssessmentCycleAdvanceDTO;
import com.moriba.skultem.application.dto.AssessmentCycleOverviewDTO;
import com.moriba.skultem.application.dto.AssessmentDTO;
import com.moriba.skultem.application.dto.AssessmentTemplateDTO;
import com.moriba.skultem.application.dto.ActiveAssessmentCycleDTO;
import com.moriba.skultem.application.dto.GradeBandDTO;
import com.moriba.skultem.application.dto.GradingScaleDTO;
import com.moriba.skultem.application.dto.StudentAssessmentDTO;
import com.moriba.skultem.application.usecase.ApproveAssessmentUseCase;
import com.moriba.skultem.application.usecase.AssignAssessmentsToTemplateUseCase;
import com.moriba.skultem.application.usecase.CreateAssessmentTemplateUseCase;
import com.moriba.skultem.application.usecase.GetActiveAssessmentCycleUseCase;
import com.moriba.skultem.application.usecase.GetAssessmentCycleOverviewUseCase;
import com.moriba.skultem.application.usecase.AdvanceAssessmentCycleUseCase;
import com.moriba.skultem.application.usecase.GradeAssessmentUseCase;
import com.moriba.skultem.application.usecase.ListAssessmentApprovalRequestUseCase;
import com.moriba.skultem.application.usecase.ListAssessmentByClassUseCase;
import com.moriba.skultem.application.usecase.ListAssessmentTemplateBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListAssessmentUseCase;
import com.moriba.skultem.application.usecase.ListStudentAssessmentTermUseCase;
import com.moriba.skultem.application.usecase.GetSchoolGradingScaleUseCase;
import com.moriba.skultem.application.usecase.ReturnAssessmentUseCase;
import com.moriba.skultem.application.usecase.SubmitAssessmentForApprovalUseCase;
import com.moriba.skultem.application.usecase.UpdateSchoolGradingScaleUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.AssessmentActionDTO;
import com.moriba.skultem.infrastructure.rest.dto.AssignAssessmentsDTO;
import com.moriba.skultem.infrastructure.rest.dto.CreateAssessmentTemplateDTO;
import com.moriba.skultem.infrastructure.rest.dto.GradeAssessmentDTO;
import com.moriba.skultem.infrastructure.rest.dto.SubmitAssessmentDTO;
import com.moriba.skultem.infrastructure.rest.dto.UpdateGradingScaleDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/assessment")
@RequiredArgsConstructor
public class AssessmentController {
    private final CreateAssessmentTemplateUseCase createAssessmentTemplateUseCase;
    private final AssignAssessmentsToTemplateUseCase assignAssessmentsToTemplateUseCase;
    private final GradeAssessmentUseCase gradeAssessmentUseCase;
    private final SubmitAssessmentForApprovalUseCase submitAssessmentForApprovalUseCase;
    private final ApproveAssessmentUseCase approveAssessmentUseCase;
    private final ReturnAssessmentUseCase returnAssessmentUseCase;
    private final ListAssessmentApprovalRequestUseCase listAssessmentApprovalRequestUseCase;
    private final ListAssessmentTemplateBySchoolUseCase listAssessmentTemplateBySchoolUseCase;
    private final ListStudentAssessmentTermUseCase listStudentAssessmentTermUseCase;
    private final ListAssessmentUseCase listAssessmentUseCase;
    private final ListAssessmentByClassUseCase assessmentByClassUseCase;
    private final GetActiveAssessmentCycleUseCase getActiveAssessmentCycleUseCase;
    private final GetAssessmentCycleOverviewUseCase getAssessmentCycleOverviewUseCase;
    private final AdvanceAssessmentCycleUseCase advanceAssessmentCycleUseCase;
    private final GetSchoolGradingScaleUseCase getSchoolGradingScaleUseCase;
    private final UpdateSchoolGradingScaleUseCase updateSchoolGradingScaleUseCase;

    @PostMapping("/template")
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<AssessmentTemplateDTO> createTemplate(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateAssessmentTemplateDTO param) {
        var res = createAssessmentTemplateUseCase.execute(school, param.name(), param.description());
        return new ApiResponse<>("success", 200, "Assessment template created successfully", res);
    }

    @PostMapping("/template/{templateId}/assignment")
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<AssessmentTemplateDTO> assignToTemplate(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String templateId,
            @Valid @RequestBody AssignAssessmentsDTO param) {
        var assignments = param.assignments().stream()
                .map(item -> new AssignAssessmentsToTemplateUseCase.AssessmentInput(item.name(), item.weight()))
                .toList();
        var res = assignAssessmentsToTemplateUseCase.execute(school, templateId, assignments);
        return new ApiResponse<>("success", 200, "Assessments assigned successfully", res);
    }

    @GetMapping("/template/{subjectId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<AssessmentCycleDTO>> getTemplateAssessment(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String subjectId,
            @RequestParam(required = true) String termId) {
        var res = listAssessmentUseCase.execute(school, subjectId, termId);
        return new ApiResponse<>("success", 200, "Assessments fetch successfully", res);
    }

    @GetMapping("/list")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<AssessmentDTO>> listAssessment(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = listAssessmentUseCase.executeAssessment(school);
        return new ApiResponse<>("success", 200, "Assessments list fetch successfully", res);
    }

    @GetMapping("/list/{classId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER', 'PARENT')")
    public ApiResponse<List<AssessmentDTO>> listAssessmentByClass(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable(name = "classId") String classId) {
        var res = assessmentByClassUseCase.execute(school, classId);
        return new ApiResponse<>("success", 200, "Assessments list fetch successfully", res);
    }

    @GetMapping("/approval/{classMasterId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<AssessmentApprovalRequestDTO>> listAssessmentApprovals(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classMasterId) {
        var res = listAssessmentApprovalRequestUseCase.execute(school, classMasterId);
        return new ApiResponse<>("success", 200, "Assessment approval request fetch successfully", res);
    }

    @PostMapping("/grade/{teacherSubjectId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<Object> gradeAssessment(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String teacherSubjectId,
            @Valid @RequestBody GradeAssessmentDTO param) {
        var grades = param.grades().stream()
                .map(item -> new GradeAssessmentUseCase.Grade(item.id(), item.score()))
                .toList();
        gradeAssessmentUseCase.execute(school, teacherSubjectId, param.assessmentId(), param.termId(), grades);
        return new ApiResponse<>("success", 200, "Assessments graded successfully", null);
    }

    @PostMapping("/submit/{teacherSubjectId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<Object> submitAssessment(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String teacherSubjectId,
            @Valid @RequestBody SubmitAssessmentDTO param) {
        submitAssessmentForApprovalUseCase.execute(school, teacherSubjectId, param.assessmentId(), param.termId(),
                param.note());
        return new ApiResponse<>("success", 200, "Assessment submitted for approval successfully", null);
    }

    @PostMapping("/approval/{approvalRequestId}/approve")
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<Object> approveAssessment(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String approvalRequestId,
            @Valid @RequestBody AssessmentActionDTO param) {
        approveAssessmentUseCase.execute(school, approvalRequestId, param.note());
        return new ApiResponse<>("success", 200, "Assessment approved successfully", null);
    }

    @PostMapping("/approval/{approvalRequestId}/return")
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<Object> returnAssessment(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String approvalRequestId,
            @Valid @RequestBody AssessmentActionDTO param) {
        returnAssessmentUseCase.execute(school, approvalRequestId, param.note());
        return new ApiResponse<>("success", 200, "Assessment returned successfully", null);
    }

    @GetMapping("/template")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<AssessmentTemplateDTO>> listTemplates(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listAssessmentTemplateBySchoolUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Assessment templates fetched successfully", list, meta);
    }

    @GetMapping("/student")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<StudentAssessmentDTO>> listStudentAssessments(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true) String teacherSubjectId,
            @RequestParam(required = true) String termId) {

        var list = listStudentAssessmentTermUseCase.execute(school, teacherSubjectId, termId);
        return new ApiResponse<>("success", 200, "Student Assessments fetched successfully", list);
    }

    @GetMapping("/cycle/active")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<ActiveAssessmentCycleDTO> getActiveCycle(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String classId) {
        var cycle = getActiveAssessmentCycleUseCase.execute(school, classId);
        return new ApiResponse<>("success", 200, "Active assessment cycle fetched successfully", cycle);
    }

    @GetMapping("/cycle/overview")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<AssessmentCycleOverviewDTO> getCycleOverview(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var overview = getAssessmentCycleOverviewUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Assessment cycle overview fetched successfully", overview);
    }

    @PostMapping("/cycle/{termId}/advance")
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<AssessmentCycleAdvanceDTO> advanceCycle(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String termId) {
        var result = advanceAssessmentCycleUseCase.execute(school, termId);
        return new ApiResponse<>("success", 200, result.message(), result);
    }

    @GetMapping("/grading-scale")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER', 'PARENT')")
    public ApiResponse<GradingScaleDTO> getGradingScale(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var scale = getSchoolGradingScaleUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Grading scale fetched successfully", scale);
    }

    @PostMapping("/grading-scale")
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<GradingScaleDTO> updateGradingScale(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody UpdateGradingScaleDTO param) {
        var bands = param.bands().stream()
                .map(item -> new GradeBandDTO(item.minScore(), item.maxScore(), item.grade()))
                .toList();
        var scale = updateSchoolGradingScaleUseCase.execute(school, bands);
        return new ApiResponse<>("success", 200, "Grading scale updated successfully", scale);
    }
}
