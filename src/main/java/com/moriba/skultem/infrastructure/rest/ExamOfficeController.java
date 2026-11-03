package com.moriba.skultem.infrastructure.rest;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.BroadsheetDTO;
import com.moriba.skultem.application.dto.ExamScheduleDTO;
import com.moriba.skultem.application.usecase.ExamScheduleUseCase;
import com.moriba.skultem.application.usecase.GetBroadsheetUseCase;
import com.moriba.skultem.application.usecase.ListExamSubmissionsUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.SaveExamScheduleDTO;
import com.moriba.skultem.infrastructure.security.SectionScoped;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Exam Office: the exam timetable and class broadsheets. Grade approval, the assessment cycle and report cards
 * keep their own endpoints (AssessmentController, ReportCardController), which the exam officer can also use.
 * SUPER_ADMIN passes any gate naming EXAM_OFFICER (PermissionService.SUPER_ADMIN_COVERS).
 */
@RestController
@RequestMapping("/api/v1/exam-office")
@RequiredArgsConstructor
public class ExamOfficeController {

    private static final String MANAGE = "@permissionService.hasAnySchoolRole(#school, 'EXAM_OFFICER', 'ADMIN', 'PRINCIPAL', 'OWNER', 'PROPRIETOR')";
    // Teachers read the timetable too - they invigilate, and need to know when their classes sit.
    private static final String VIEW = "@permissionService.hasAnySchoolRole(#school, 'EXAM_OFFICER', 'ADMIN', 'PRINCIPAL', 'OWNER', 'PROPRIETOR', 'TEACHER')";

    private final ExamScheduleUseCase examScheduleUseCase;
    private final GetBroadsheetUseCase getBroadsheetUseCase;
    private final ListExamSubmissionsUseCase listExamSubmissionsUseCase;

    @SectionScoped
    @GetMapping("/schedule")
    @PreAuthorize(VIEW + " and @sectionScope.clazz(#school, #classId)")
    public ApiResponse<List<ExamScheduleDTO>> listSchedule(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @RequestParam String termId,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false, defaultValue = "false") boolean mine) {
        var res = examScheduleUseCase.list(school, termId, classId, mine ? userId : null);
        return new ApiResponse<>("success", 200, "Exam timetable fetched successfully", res);
    }

    @SectionScoped
    @PostMapping("/schedule")
    @PreAuthorize(MANAGE + " and @sectionScope.clazz(#school, #param.classId())")
    public ApiResponse<ExamScheduleDTO> createSchedule(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SaveExamScheduleDTO param) {
        var res = examScheduleUseCase.create(school, toInput(param));
        return new ApiResponse<>("success", 200, "Exam paper added", res);
    }

    @SectionScoped
    @PutMapping("/schedule/{id}")
    @PreAuthorize(MANAGE + " and @sectionScope.examSchedule(#school, #id) and @sectionScope.clazz(#school, #param.classId())")
    public ApiResponse<ExamScheduleDTO> updateSchedule(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody SaveExamScheduleDTO param) {
        var res = examScheduleUseCase.update(school, id, toInput(param));
        return new ApiResponse<>("success", 200, "Exam paper updated", res);
    }

    @SectionScoped
    @DeleteMapping("/schedule/{id}")
    @PreAuthorize(MANAGE + " and @sectionScope.examSchedule(#school, #id)")
    public ApiResponse<Void> deleteSchedule(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        examScheduleUseCase.delete(school, id);
        return new ApiResponse<>("success", 200, "Exam paper removed", null);
    }

    // termId blank = the active term.
    @SectionScoped
    @GetMapping("/submissions")
    @PreAuthorize(MANAGE)
    public ApiResponse<ListExamSubmissionsUseCase.Result> submissions(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String termId) {
        var res = listExamSubmissionsUseCase.execute(school, termId);
        return new ApiResponse<>("success", 200, "Submissions fetched successfully", res);
    }

    @SectionScoped
    @GetMapping("/broadsheet")
    @PreAuthorize(MANAGE + " and @sectionScope.clazz(#school, #classId)")
    public ApiResponse<BroadsheetDTO> broadsheet(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String classId,
            @RequestParam String termId,
            @RequestParam(required = false) String sectionId,
            @RequestParam(required = false) String streamId) {
        var res = getBroadsheetUseCase.execute(school, classId, termId, sectionId, streamId);
        return new ApiResponse<>("success", 200, "Broadsheet fetched successfully", res);
    }

    private static ExamScheduleUseCase.Input toInput(SaveExamScheduleDTO p) {
        return new ExamScheduleUseCase.Input(p.termId(), p.classId(), p.subjectId(), p.paper(), p.examDate(),
                p.startTime(), p.endTime(), p.venue(), p.invigilatorId(), p.notes());
    }
}
