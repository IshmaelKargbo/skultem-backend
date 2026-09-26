package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.infrastructure.security.SectionScoped;

import com.moriba.skultem.infrastructure.security.SectionNeutral;

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
import com.moriba.skultem.application.dto.AssessmentApprovalSummaryDTO;
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
import com.moriba.skultem.application.usecase.ReopenAssessmentCycleUseCase;
import com.moriba.skultem.application.usecase.ReturnAssessmentUseCase;
import com.moriba.skultem.application.usecase.SubmitAssessmentForApprovalUseCase;
import com.moriba.skultem.application.usecase.UpdateSchoolGradingScaleUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.AssessmentActionDTO;
import com.moriba.skultem.infrastructure.rest.dto.AssignAssessmentsDTO;
import com.moriba.skultem.infrastructure.rest.dto.CreateAssessmentTemplateDTO;
import com.moriba.skultem.infrastructure.rest.dto.GradeAssessmentDTO;
import com.moriba.skultem.infrastructure.rest.dto.ReopenAssessmentDTO;
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
    private final ReopenAssessmentCycleUseCase reopenAssessmentCycleUseCase;
    private final com.moriba.skultem.application.usecase.RecordContinuousAssessmentUseCase recordContinuousAssessmentUseCase;
    private final com.moriba.skultem.application.usecase.SubmitContinuousCaUseCase submitContinuousCaUseCase;
    private final com.moriba.skultem.application.usecase.LockContinuousWeekUseCase lockContinuousWeekUseCase;
    private final com.moriba.skultem.application.usecase.UnlockContinuousWeekUseCase unlockContinuousWeekUseCase;

    // A template (e.g. "Mid-Term Test", pass mark 40%) is a school-wide reusable definition with no
    // level of its own - see AssessmentTemplate - so it's section-neutral like the reads below.
    @SectionNeutral
    @PostMapping("/template")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER')")
    public ApiResponse<AssessmentTemplateDTO> createTemplate(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateAssessmentTemplateDTO param) {
        var res = createAssessmentTemplateUseCase.execute(school, param.name(), param.passMark(), param.description());
        return new ApiResponse<>("success", 200, "Assessment template created successfully", res);
    }

    @SectionNeutral
    @PostMapping("/template/{templateId}/assignment")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER')")
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

    @SectionNeutral
    @GetMapping("/template/{subjectId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER')")
    public ApiResponse<List<AssessmentCycleDTO>> getTemplateAssessment(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String subjectId,
            @RequestParam(required = true) String termId) {
        var res = listAssessmentUseCase.execute(school, subjectId, termId);
        return new ApiResponse<>("success", 200, "Assessments fetch successfully", res);
    }

    @SectionNeutral
    @GetMapping("/list")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER')")
    public ApiResponse<List<AssessmentDTO>> listAssessment(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = listAssessmentUseCase.executeAssessment(school);
        return new ApiResponse<>("success", 200, "Assessments list fetch successfully", res);
    }

    @SectionScoped
    @GetMapping("/list/{classId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER', 'PARENT') and @sectionScope.clazz(#school, #classId)")
    public ApiResponse<List<AssessmentDTO>> listAssessmentByClass(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable(name = "classId") String classId) {
        var res = assessmentByClassUseCase.execute(school, classId);
        return new ApiResponse<>("success", 200, "Assessments list fetch successfully", res);
    }

    @SectionScoped
    @GetMapping("/approval")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER')")
    public ApiResponse<List<AssessmentApprovalRequestDTO>> listAllAssessmentApprovals(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listAssessmentApprovalRequestUseCase.executeForSchool(school, status, query, academicYearId, page,
                size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Assessment approval request fetch successfully", list, meta);
    }

    @SectionScoped
    @GetMapping("/approval/summary")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER')")
    public ApiResponse<AssessmentApprovalSummaryDTO> allAssessmentApprovalSummary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = listAssessmentApprovalRequestUseCase.summaryForSchool(school);
        return new ApiResponse<>("success", 200, "Assessment approval summary fetch successfully", res);
    }

    @SectionScoped
    @GetMapping("/approval/{classMasterId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER')")
    public ApiResponse<List<AssessmentApprovalRequestDTO>> listAssessmentApprovals(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classMasterId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listAssessmentApprovalRequestUseCase.execute(school, classMasterId, status, query, academicYearId,
                page, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Assessment approval request fetch successfully", list, meta);
    }

    @SectionScoped
    @GetMapping("/approval/request/{approvalRequestId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER') and @sectionScope.approvalRequest(#school, #approvalRequestId)")
    public ApiResponse<AssessmentApprovalRequestDTO> getAssessmentApprovalRequest(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String approvalRequestId) {
        var res = listAssessmentApprovalRequestUseCase.getOne(school, approvalRequestId);
        return new ApiResponse<>("success", 200, "Assessment approval request fetched successfully", res);
    }

    @SectionScoped
    @GetMapping("/approval/{classMasterId}/summary")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER')")
    public ApiResponse<AssessmentApprovalSummaryDTO> assessmentApprovalSummary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classMasterId) {
        var res = listAssessmentApprovalRequestUseCase.summary(school, classMasterId);
        return new ApiResponse<>("success", 200, "Assessment approval summary fetch successfully", res);
    }

    @SectionScoped
    @GetMapping("/approval/me")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER')")
    public ApiResponse<List<AssessmentApprovalRequestDTO>> listMeAssessmentApprovals(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listAssessmentApprovalRequestUseCase.executeByUser(school, userId, status, query, academicYearId,
                page, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());
        return new ApiResponse<>("success", 200, "Assessment approval request fetch successfully", list, meta);
    }

    @SectionScoped
    @GetMapping("/approval/me/summary")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER')")
    public ApiResponse<AssessmentApprovalSummaryDTO> meAssessmentApprovalSummary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId) {
        var res = listAssessmentApprovalRequestUseCase.summaryByUser(school, userId);
        return new ApiResponse<>("success", 200, "Assessment approval summary fetch successfully", res);
    }

    // Recording an assessment opened as continuous assessment (CA recordings + formal test). Teachers use the
    // structure the assessment froze when it opened; nothing here can change it.
    @SectionScoped
    @PostMapping("/continuous/{teacherSubjectId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER') and @sectionScope.teacherSubject(#school, #teacherSubjectId)")
    public ApiResponse<Object> recordContinuous(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String teacherSubjectId,
            @Valid @RequestBody com.moriba.skultem.infrastructure.rest.dto.RecordContinuousAssessmentDTO param) {
        var records = param.records().stream()
                .map(r -> new com.moriba.skultem.application.usecase.RecordContinuousAssessmentUseCase.StudentRecord(
                        r.scoreId(),
                        r.entries() == null ? java.util.List.of()
                                : r.entries().stream()
                                        .map(e -> new com.moriba.skultem.application.usecase.RecordContinuousAssessmentUseCase.CaValue(
                                                e.entryNumber(), e.score()))
                                        .toList(),
                        r.formalScore()))
                .toList();
        int saved = recordContinuousAssessmentUseCase.execute(school, teacherSubjectId, param.assessmentId(),
                param.termId(), records);
        return new ApiResponse<>("success", 200, saved + " student(s) recorded", null);
    }

    // Locks a completed CA recording (every student has it) so it can't be edited afterwards.
    @SectionScoped
    @PostMapping("/continuous/{teacherSubjectId}/lock-week")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER') and @sectionScope.teacherSubject(#school, #teacherSubjectId)")
    public ApiResponse<Object> lockContinuousWeek(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String teacherSubjectId,
            @Valid @RequestBody com.moriba.skultem.infrastructure.rest.dto.ContinuousWeekDTO param) {
        lockContinuousWeekUseCase.execute(school, teacherSubjectId, param.assessmentId(), param.termId(), param.week());
        return new ApiResponse<>("success", 200, "Recording locked", null);
    }

    // The only way back into a locked recording - administrators only, with a reason (kept on the audit trail).
    @SectionScoped
    @PostMapping("/continuous/{teacherSubjectId}/unlock-week")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER') and @sectionScope.teacherSubject(#school, #teacherSubjectId)")
    public ApiResponse<Object> unlockContinuousWeek(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String teacherSubjectId,
            @Valid @RequestBody com.moriba.skultem.infrastructure.rest.dto.ContinuousWeekDTO param) {
        unlockContinuousWeekUseCase.execute(school, teacherSubjectId, param.assessmentId(), param.termId(),
                param.week(), param.reason());
        return new ApiResponse<>("success", 200, "Recording unlocked", null);
    }

    // Closes the CA step of a continuous assessment - every recording in for every student - so the formal test
    // can be entered next.
    @SectionScoped
    @PostMapping("/continuous/{teacherSubjectId}/submit-ca")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER') and @sectionScope.teacherSubject(#school, #teacherSubjectId)")
    public ApiResponse<Object> submitContinuousCa(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String teacherSubjectId,
            @Valid @RequestBody com.moriba.skultem.infrastructure.rest.dto.SubmitContinuousCaDTO param) {
        submitContinuousCaUseCase.execute(school, teacherSubjectId, param.assessmentId(), param.termId());
        return new ApiResponse<>("success", 200, "CA submitted - the formal test can now be entered", null);
    }

    @SectionScoped
    @PostMapping("/grade/{teacherSubjectId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER') and @sectionScope.teacherSubject(#school, #teacherSubjectId)")
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

    @SectionScoped
    @PostMapping("/submit/{teacherSubjectId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER') and @sectionScope.teacherSubject(#school, #teacherSubjectId)")
    public ApiResponse<Object> submitAssessment(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String teacherSubjectId,
            @Valid @RequestBody SubmitAssessmentDTO param) {
        submitAssessmentForApprovalUseCase.execute(school, teacherSubjectId, param.assessmentId(), param.termId(),
                param.note());
        return new ApiResponse<>("success", 200, "Assessment submitted for approval successfully", null);
    }

    @SectionScoped
    @PostMapping("/approval/{approvalRequestId}/approve")
    @PreAuthorize("@permissionService.canReviewAssessmentApproval(#school, #approvalRequestId) and @sectionScope.approvalRequest(#school, #approvalRequestId)")
    public ApiResponse<Object> approveAssessment(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String approvalRequestId,
            @Valid @RequestBody AssessmentActionDTO param) {
        approveAssessmentUseCase.execute(school, approvalRequestId, param.note());
        return new ApiResponse<>("success", 200, "Assessment approved successfully", null);
    }

    @SectionScoped
    @PostMapping("/reopen/{teacherSubjectId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.teacherSubject(#school, #teacherSubjectId)")
    public ApiResponse<AssessmentCycleDTO> reopenAssessment(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String teacherSubjectId,
            @Valid @RequestBody ReopenAssessmentDTO param) {
        var res = reopenAssessmentCycleUseCase.execute(school, teacherSubjectId, param.assessmentId(),
                param.termId(), param.note());
        return new ApiResponse<>("success", 200, "Assessment reopened for editing successfully", res);
    }

    @SectionScoped
    @PostMapping("/approval/{approvalRequestId}/return")
    @PreAuthorize("@permissionService.canReviewAssessmentApproval(#school, #approvalRequestId) and @sectionScope.approvalRequest(#school, #approvalRequestId)")
    public ApiResponse<Object> returnAssessment(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String approvalRequestId,
            @Valid @RequestBody AssessmentActionDTO param) {
        returnAssessmentUseCase.execute(school, approvalRequestId, param.note());
        return new ApiResponse<>("success", 200, "Assessment returned successfully", null);
    }

    @SectionNeutral
    @GetMapping("/template")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER')")
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

    @SectionScoped
    @GetMapping("/student")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER') and @sectionScope.teacherSubject(#school, #teacherSubjectId)")
    public ApiResponse<List<StudentAssessmentDTO>> listStudentAssessments(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true) String teacherSubjectId,
            @RequestParam(required = true) String termId) {

        var list = listStudentAssessmentTermUseCase.execute(school, teacherSubjectId, termId);
        return new ApiResponse<>("success", 200, "Student Assessments fetched successfully", list);
    }

    @SectionScoped
    @GetMapping("/cycle/active")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER') and @sectionScope.clazz(#school, #classId)")
    public ApiResponse<ActiveAssessmentCycleDTO> getActiveCycle(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String academicYearId) {
        var cycle = getActiveAssessmentCycleUseCase.execute(school, classId, academicYearId);
        return new ApiResponse<>("success", 200, "Active assessment cycle fetched successfully", cycle);
    }

    @SectionScoped
    @GetMapping("/cycle/overview")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER')")
    public ApiResponse<AssessmentCycleOverviewDTO> getCycleOverview(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String academicYearId) {
        var overview = getAssessmentCycleOverviewUseCase.execute(school, academicYearId);
        return new ApiResponse<>("success", 200, "Assessment cycle overview fetched successfully", overview);
    }

    // Sections run their assessments separately, so a section moves on its own (sectionId). A section-limited admin can
    // only move their own; a school without sections omits it.
    @SectionScoped
    @PostMapping("/cycle/{termId}/advance")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.managementSection(#sectionId)")
    public ApiResponse<AssessmentCycleAdvanceDTO> advanceCycle(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String termId,
            @RequestParam(required = false) String sectionId) {
        var result = advanceAssessmentCycleUseCase.execute(school, termId, sectionId);
        return new ApiResponse<>("success", 200, result.message(), result);
    }

    @SectionNeutral
    @GetMapping("/grading-scale")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'PARENT')")
    public ApiResponse<GradingScaleDTO> getGradingScale(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var scale = getSchoolGradingScaleUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Grading scale fetched successfully", scale);
    }

    @PostMapping("/grading-scale")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
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
