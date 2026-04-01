package com.moriba.skultem.infrastructure.rest;

import java.time.LocalDate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.infrastructure.report.ReportExportService;
import com.moriba.skultem.infrastructure.report.ReportFile;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.RunReportDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/report/export")
@RequiredArgsConstructor
public class ReportExportController {

    private final ReportExportService reportExportService;

    @GetMapping("/payments")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ResponseEntity<byte[]> exportPayments(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return toResponse(reportExportService.exportPayments(school, format, classId, startDate, endDate));
    }

    @GetMapping("/attendance")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'TEACHER')")
    public ResponseEntity<byte[]> exportAttendance(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String classSessionId,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return toResponse(reportExportService.exportAttendance(school, classSessionId, format, startDate, endDate));
    }

    @GetMapping("/behaviour")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'TEACHER')")
    public ResponseEntity<byte[]> exportBehaviour(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String classId,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return toResponse(reportExportService.exportBehaviour(school, classId, format, startDate, endDate));
    }

    @GetMapping("/fees")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> exportFees(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return toResponse(reportExportService.exportFees(school, format, classId, startDate, endDate));
    }

    @GetMapping("/grades")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'TEACHER')")
    public ResponseEntity<byte[]> exportGrades(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String teacherSubjectId,
            @RequestParam String termId,
            @RequestParam(defaultValue = "csv") String format) {
        return toResponse(reportExportService.exportGrades(school, teacherSubjectId, termId, format));
    }

    private ResponseEntity<byte[]> toResponse(ReportFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, file.contentType())
                .body(file.data());
    }

    @PostMapping("/run")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER', 'PARENT')")
    public ApiResponse<Object> runReport(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam int page,
            @RequestParam int size,
            @RequestBody RunReportDTO param) {
        var res = reportExportService.runReport(school, param, page, size);
        var data = res.getData();
        var meta = res.getMeta();
        return new ApiResponse<>("success", 200, "Report generated successfully", data, meta);
    }
}
