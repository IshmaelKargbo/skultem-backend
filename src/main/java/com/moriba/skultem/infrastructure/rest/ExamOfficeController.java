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
import com.moriba.skultem.application.dto.ExamTimetableDTO;
import com.moriba.skultem.application.usecase.ExamTimetableUseCase;
import com.moriba.skultem.application.usecase.GetBroadsheetUseCase;
import com.moriba.skultem.application.usecase.ListExamSubmissionsUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.SaveExamPaperDTO;
import com.moriba.skultem.infrastructure.rest.dto.SaveExamTimetableDTO;
import com.moriba.skultem.infrastructure.security.PermissionService;
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

    private final ExamTimetableUseCase examTimetableUseCase;
    private final GetBroadsheetUseCase getBroadsheetUseCase;
    private final ListExamSubmissionsUseCase listExamSubmissionsUseCase;
    private final PermissionService permissionService;

    // ---- Exam timetables: one per assessment per term. Teachers only see published ones.

    @SectionScoped
    @GetMapping("/timetables")
    @PreAuthorize(VIEW)
    public ApiResponse<List<ExamTimetableDTO.Summary>> listTimetables(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String termId) {
        var res = examTimetableUseCase.listForTerm(school, termId, canManage(school));
        return new ApiResponse<>("success", 200, "Exam timetables fetched successfully", res);
    }

    @SectionScoped
    @GetMapping("/timetables/{id}")
    @PreAuthorize(VIEW + " and @sectionScope.examTimetable(#school, #id) and @sectionScope.clazz(#school, #classId)")
    public ApiResponse<ExamTimetableDTO> getTimetable(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false, defaultValue = "false") boolean mine) {
        var res = examTimetableUseCase.get(school, id, classId, mine ? userId : null, canManage(school));
        return new ApiResponse<>("success", 200, "Exam timetable fetched successfully", res);
    }

    // A parent's child: their class's papers on every published timetable this year.
    @SectionScoped
    @GetMapping("/timetables/student/{studentId}")
    @PreAuthorize("@permissionService.isParentOfStudent(#school, #studentId) and @sectionScope.student(#school, #studentId)")
    public ApiResponse<List<ExamTimetableDTO>> studentTimetables(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String studentId) {
        var res = examTimetableUseCase.forStudent(school, studentId);
        return new ApiResponse<>("success", 200, "Exam timetables fetched successfully", res);
    }

    @SectionScoped
    @PostMapping("/timetables")
    @PreAuthorize(MANAGE)
    public ApiResponse<ExamTimetableDTO> createTimetable(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SaveExamTimetableDTO param) {
        var res = examTimetableUseCase.create(school, toInput(param));
        return new ApiResponse<>("success", 200, "Exam timetable set up", res);
    }

    @SectionScoped
    @PutMapping("/timetables/{id}")
    @PreAuthorize(MANAGE + " and @sectionScope.examTimetable(#school, #id)")
    public ApiResponse<ExamTimetableDTO> updateTimetable(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody SaveExamTimetableDTO param) {
        var res = examTimetableUseCase.update(school, id, toInput(param));
        return new ApiResponse<>("success", 200, "Exam timetable updated", res);
    }

    @SectionScoped
    @PostMapping("/timetables/{id}/publish")
    @PreAuthorize(MANAGE + " and @sectionScope.examTimetable(#school, #id)")
    public ApiResponse<ExamTimetableDTO> publishTimetable(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = examTimetableUseCase.publish(school, id);
        return new ApiResponse<>("success", 200, "Exam timetable published", res);
    }

    @SectionScoped
    @PostMapping("/timetables/{id}/unpublish")
    @PreAuthorize(MANAGE + " and @sectionScope.examTimetable(#school, #id)")
    public ApiResponse<ExamTimetableDTO> unpublishTimetable(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = examTimetableUseCase.unpublish(school, id);
        return new ApiResponse<>("success", 200, "Exam timetable moved back to draft", res);
    }

    @SectionScoped
    @DeleteMapping("/timetables/{id}")
    @PreAuthorize(MANAGE + " and @sectionScope.examTimetable(#school, #id)")
    public ApiResponse<Void> deleteTimetable(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        examTimetableUseCase.delete(school, id);
        return new ApiResponse<>("success", 200, "Exam timetable deleted", null);
    }

    @SectionScoped
    @PostMapping("/timetables/{id}/papers")
    @PreAuthorize(MANAGE + " and @sectionScope.examTimetable(#school, #id)")
    public ApiResponse<ExamTimetableDTO> addPaper(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody SaveExamPaperDTO param) {
        var res = examTimetableUseCase.addPaper(school, id, toInput(param));
        return new ApiResponse<>("success", 200, "Exam paper added", res);
    }

    @SectionScoped
    @PutMapping("/papers/{id}")
    @PreAuthorize(MANAGE + " and @sectionScope.examPaper(#school, #id)")
    public ApiResponse<ExamTimetableDTO> updatePaper(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody SaveExamPaperDTO param) {
        var res = examTimetableUseCase.updatePaper(school, id, toInput(param));
        return new ApiResponse<>("success", 200, "Exam paper updated", res);
    }

    @SectionScoped
    @DeleteMapping("/papers/{id}")
    @PreAuthorize(MANAGE + " and @sectionScope.examPaper(#school, #id)")
    public ApiResponse<ExamTimetableDTO> deletePaper(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = examTimetableUseCase.deletePaper(school, id);
        return new ApiResponse<>("success", 200, "Exam paper removed", res);
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

    private boolean canManage(String school) {
        return permissionService.hasAnySchoolRole(school, "EXAM_OFFICER", "ADMIN", "PRINCIPAL", "OWNER", "PROPRIETOR");
    }

    private static ExamTimetableUseCase.TimetableInput toInput(SaveExamTimetableDTO p) {
        return new ExamTimetableUseCase.TimetableInput(p.termId(), p.assessmentId(), p.title(), p.startDate(),
                p.endDate(), p.instructions());
    }

    private static ExamTimetableUseCase.PaperInput toInput(SaveExamPaperDTO p) {
        return new ExamTimetableUseCase.PaperInput(p.subjectId(), p.paper(), p.examDate(), p.startTime(),
                p.endTime(), p.classIds(), p.invigilatorIds(), p.venue(), p.notes());
    }
}
