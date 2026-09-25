package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.infrastructure.security.SectionScoped;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.AttendanceDTO;
import com.moriba.skultem.application.dto.AttendanceHistoryDTO;
import com.moriba.skultem.application.dto.ClassAttendanceSummaryDTO;
import com.moriba.skultem.application.dto.ClassSessionAttendanceDTO;
import com.moriba.skultem.application.dto.InspectionReportDTO;
import com.moriba.skultem.application.dto.InspectionReportType;
import com.moriba.skultem.application.dto.StudentAttendanceSummaryDTO;
import com.moriba.skultem.application.dto.TermAttendanceSummaryDTO;
import com.moriba.skultem.application.usecase.AttendanceReportUseCase;
import com.moriba.skultem.application.usecase.DeleteAttendanceUseCase;
import com.moriba.skultem.application.usecase.DeleteClassSessionAttendanceUseCase;
import com.moriba.skultem.application.usecase.GenerateClassAttendanceSummaryUseCase;
import com.moriba.skultem.application.usecase.GenerateDailyAttendanceRegisterUseCase;
import com.moriba.skultem.application.usecase.GenerateInspectionReportUseCase;
import com.moriba.skultem.application.usecase.GenerateMonthlyAttendanceSummaryUseCase;
import com.moriba.skultem.application.usecase.GenerateTermAttendanceSummaryUseCase;
import com.moriba.skultem.application.usecase.GetAttendanceUseCase;
import com.moriba.skultem.application.usecase.GetClassSessionAttendanceUseCase;
import com.moriba.skultem.application.usecase.ListAttendanceBySchoolUseCase;
import com.moriba.skultem.application.usecase.MarkClassSessionAttendanceUseCase;
import com.moriba.skultem.application.usecase.MarkClassSessionAttendanceUseCase.MarkRecord;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.ClassAttendanceMarkDTO;
import com.moriba.skultem.infrastructure.rest.dto.MarkClassAttendanceDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final GetAttendanceUseCase getAttendanceUseCase;
    private final ListAttendanceBySchoolUseCase listAttendanceBySchoolUseCase;
    private final DeleteAttendanceUseCase deleteAttendanceUseCase;
    private final MarkClassSessionAttendanceUseCase markClassSessionAttendanceUseCase;
    private final DeleteClassSessionAttendanceUseCase deleteClassSessionAttendanceUseCase;
    private final AttendanceReportUseCase attendanceReportUseCase;
    private final GetClassSessionAttendanceUseCase getClassSessionAttendanceUseCase;
    private final GenerateDailyAttendanceRegisterUseCase generateDailyAttendanceRegisterUseCase;
    private final GenerateMonthlyAttendanceSummaryUseCase generateMonthlyAttendanceSummaryUseCase;
    private final GenerateTermAttendanceSummaryUseCase generateTermAttendanceSummaryUseCase;
    private final GenerateInspectionReportUseCase generateInspectionReportUseCase;
    private final GenerateClassAttendanceSummaryUseCase generateClassAttendanceSummaryUseCase;

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<AttendanceDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getAttendanceUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Attendance fetched successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<AttendanceDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {

        var res = listAttendanceBySchoolUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Attendance fetched successfully", list, meta);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        deleteAttendanceUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Attendance deleted successfully", null);
    }

    @SectionScoped
    @PostMapping("/session/{classSessionId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER') and @sectionScope.classSession(#school, #classSessionId)")
    public ApiResponse<List<AttendanceDTO>> markClassSession(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classSessionId,
            @Valid @RequestBody MarkClassAttendanceDTO param) {
        var records = param.records().stream().map(this::toMarkRecord).toList();
        var res = markClassSessionAttendanceUseCase.execute(
                school, classSessionId, param.date(), param.holiday(), records);
        return new ApiResponse<>("success", 200, "Class attendance marked successfully", res);
    }

    @SectionScoped
    @DeleteMapping("/session/{classSessionId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER') and @sectionScope.classSession(#school, #classSessionId)")
    public ApiResponse<Void> deleteClassSession(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classSessionId,
            @RequestParam(required = true) java.time.LocalDate date) {
        deleteClassSessionAttendanceUseCase.execute(school, classSessionId, date);
        return new ApiResponse<>("success", 200, "Class attendance deleted successfully", null);
    }

    @SectionScoped
    @GetMapping("/session/{classSessionId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER') and @sectionScope.classSession(#school, #classSessionId)")
    public ApiResponse<ClassSessionAttendanceDTO> getClassSessionSheet(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classSessionId,
            @RequestParam(required = true) java.time.LocalDate date) {
        var res = getClassSessionAttendanceUseCase.execute(school, classSessionId, date);
        return new ApiResponse<>("success", 200, "Class attendance sheet fetched successfully", res);
    }

    @SectionScoped
    @GetMapping("/session/report/{classSessionId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER') and @sectionScope.classSession(#school, #classSessionId)")
    public ApiResponse<List<AttendanceHistoryDTO>> getClassSessionReport(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classSessionId,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = attendanceReportUseCase.execute(school, classSessionId, academicYearId, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Class attendance sheet fetched successfully", list, meta);
    }

    @SectionScoped
    @GetMapping("/register/{classSessionId}")
    @PreAuthorize("(@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') "
            + "or @permissionService.canAccessClassSessionAsTeacher(#school, #classSessionId)) "
            + "and @sectionScope.classSession(#school, #classSessionId)")
    public ApiResponse<ClassSessionAttendanceDTO> getDailyRegister(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classSessionId,
            @RequestParam(required = true) LocalDate date) {
        var res = generateDailyAttendanceRegisterUseCase.execute(school, classSessionId, date);
        return new ApiResponse<>("success", 200, "Daily attendance register fetched successfully", res);
    }

    @SectionScoped
    @GetMapping("/summary/monthly")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.classSession(#school, #classSessionId)")
    public ApiResponse<List<StudentAttendanceSummaryDTO>> getMonthlySummary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true) String classSessionId,
            @RequestParam(required = true) Integer year,
            @RequestParam(required = true) Integer month) {
        var res = generateMonthlyAttendanceSummaryUseCase.execute(school, classSessionId, YearMonth.of(year, month));
        return new ApiResponse<>("success", 200, "Monthly attendance summary fetched successfully", res);
    }

    @SectionScoped
    @GetMapping("/summary/term")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.classSession(#school, #classSessionId)")
    public ApiResponse<TermAttendanceSummaryDTO> getTermSummary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true) String classSessionId,
            @RequestParam(required = true) String termId) {
        var res = generateTermAttendanceSummaryUseCase.execute(school, classSessionId, termId);
        return new ApiResponse<>("success", 200, "Term attendance summary fetched successfully", res);
    }

    @GetMapping("/summary/class")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<ClassAttendanceSummaryDTO> getClassSummary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = false) String termId) {
        var res = generateClassAttendanceSummaryUseCase.execute(school, academicYearId, termId);
        return new ApiResponse<>("success", 200, "Class attendance summary fetched successfully", res);
    }

    @SectionScoped
    @GetMapping("/inspection-report")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.classSession(#school, #classSessionId)")
    public ApiResponse<InspectionReportDTO> getInspectionReport(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true) InspectionReportType reportType,
            @RequestParam(required = true) String classSessionId,
            @RequestParam(required = false) String termId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        var res = generateInspectionReportUseCase.execute(school, reportType, classSessionId, termId, date, year,
                month);
        return new ApiResponse<>("success", 200, "Inspection report generated successfully", res);
    }

    private MarkRecord toMarkRecord(ClassAttendanceMarkDTO param) {
        return new MarkRecord(param.studentId(), param.present(), param.excused(), param.late(), param.reason());
    }
}
