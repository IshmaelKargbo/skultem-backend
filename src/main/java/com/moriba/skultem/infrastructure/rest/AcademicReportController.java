package com.moriba.skultem.infrastructure.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.AcademicReportDTO;
import com.moriba.skultem.application.dto.AcademicTrendDTO;
import com.moriba.skultem.application.dto.AssessmentCompletionRowDTO;
import com.moriba.skultem.application.dto.ClassAcademicAttentionDTO;
import com.moriba.skultem.application.dto.StudentDemographicsDTO;
import com.moriba.skultem.application.dto.StudentPerformanceTrendDTO;
import com.moriba.skultem.application.dto.WeeklyGenderAttendanceDTO;
import com.moriba.skultem.application.usecase.GenerateStudentDemographicsReportUseCase;
import com.moriba.skultem.application.usecase.GenerateStudentsRequiringAttentionUseCase;
import com.moriba.skultem.application.usecase.GenerateWeeklyGenderAttendanceReportUseCase;
import com.moriba.skultem.application.usecase.GetAcademicTrendUseCase;
import com.moriba.skultem.application.usecase.GetAssessmentCompletionReportUseCase;
import com.moriba.skultem.application.usecase.GetClassAcademicPerformanceUseCase;
import com.moriba.skultem.application.usecase.GetStudentPerformanceTrendUseCase;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

// Management-facing academic reporting, on top of the existing assessment/attendance/student
// data - no new persisted report state, everything here is calculated live from authoritative
// records. Scoped to ADMIN/OWNER/PROPRIETOR only, per the requirement that this report is for
// school management rather than teachers or parents. classId is optional on every endpoint below
// that accepts it - omit it for a whole-school report.
@RestController
@RequestMapping("/api/v1/report/academic")
@RequiredArgsConstructor
public class AcademicReportController {

    private static final String MANAGEMENT_ROLES = "'ADMIN', 'OWNER', 'PROPRIETOR'";

    private final GetClassAcademicPerformanceUseCase getClassAcademicPerformanceUseCase;
    private final GetStudentPerformanceTrendUseCase getStudentPerformanceTrendUseCase;
    private final GetAcademicTrendUseCase getAcademicTrendUseCase;
    private final GetAssessmentCompletionReportUseCase getAssessmentCompletionReportUseCase;
    private final GenerateStudentsRequiringAttentionUseCase generateStudentsRequiringAttentionUseCase;
    private final GenerateWeeklyGenderAttendanceReportUseCase generateWeeklyGenderAttendanceReportUseCase;
    private final GenerateStudentDemographicsReportUseCase generateStudentDemographicsReportUseCase;

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, " + MANAGEMENT_ROLES + ")")
    public ApiResponse<AcademicReportDTO> getAcademicReport(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = false) String termId,
            @RequestParam(required = false) Level level,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        var res = getClassAcademicPerformanceUseCase.execute(school, classId, academicYearId, termId, subjectId,
                level, page, size);

        Map<String, Object> meta = Map.of(
                "page", res.studentsPage(),
                "size", res.studentsSize(),
                "count", res.studentsTotal());

        return new ApiResponse<>("success", 200, "Academic report fetched successfully", res, meta);
    }

    @GetMapping("/student/{enrollmentId}/trend")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, " + MANAGEMENT_ROLES + ")")
    public ApiResponse<StudentPerformanceTrendDTO> getStudentPerformanceTrend(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String enrollmentId,
            @RequestParam(required = true) String termId) {
        var res = getStudentPerformanceTrendUseCase.execute(school, enrollmentId, termId);
        return new ApiResponse<>("success", 200, "Student performance trend fetched successfully", res);
    }

    @GetMapping("/trend")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, " + MANAGEMENT_ROLES + ")")
    public ApiResponse<AcademicTrendDTO> getAcademicTrend(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = false) String termId) {
        var res = getAcademicTrendUseCase.execute(school, classId, subjectId, academicYearId, termId);
        return new ApiResponse<>("success", 200, "Academic trend fetched successfully", res);
    }

    @GetMapping("/completion")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, " + MANAGEMENT_ROLES + ")")
    public ApiResponse<List<AssessmentCompletionRowDTO>> getAssessmentCompletion(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = false) String termId,
            @RequestParam(required = false) Level level,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        var res = getAssessmentCompletionReportUseCase.execute(school, classId, subjectId, academicYearId, termId,
                level, page, size);

        Map<String, Object> meta = Map.of(
                "page", res.page(),
                "size", res.size(),
                "count", res.total());

        return new ApiResponse<>("success", 200, "Assessment completion report fetched successfully", res.rows(),
                meta);
    }

    @GetMapping("/attention")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, " + MANAGEMENT_ROLES + ")")
    public ApiResponse<ClassAcademicAttentionDTO> getStudentsRequiringAttention(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = false) String termId,
            @RequestParam(required = false) Level level,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        var res = generateStudentsRequiringAttentionUseCase.execute(school, classId, academicYearId, termId, level,
                page, size);

        Map<String, Object> meta = Map.of(
                "page", res.page(),
                "size", res.size(),
                "count", res.flaggedCount());

        return new ApiResponse<>("success", 200, "Students requiring attention fetched successfully", res, meta);
    }

    @GetMapping("/class/{classId}/attendance/weekly-gender")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, " + MANAGEMENT_ROLES + ")")
    public ApiResponse<WeeklyGenderAttendanceDTO> getWeeklyGenderAttendance(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classId,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = false) LocalDate weekOf) {
        LocalDate resolvedWeek = weekOf != null ? weekOf : LocalDate.now();
        var res = generateWeeklyGenderAttendanceReportUseCase.execute(school, classId, academicYearId,
                resolvedWeek);
        return new ApiResponse<>("success", 200, "Weekly gender attendance fetched successfully", res);
    }

    @GetMapping("/demographics")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, " + MANAGEMENT_ROLES + ")")
    public ApiResponse<StudentDemographicsDTO> getStudentDemographics(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) Level level) {
        var res = generateStudentDemographicsReportUseCase.execute(school, academicYearId, classId, level);
        return new ApiResponse<>("success", 200, "Student demographics fetched successfully", res);
    }
}
