package com.moriba.skultem.infrastructure.rest;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.ActivityDTO;
import com.moriba.skultem.application.dto.DashboardDTO;
import com.moriba.skultem.application.dto.MonthlyEnrollmentDTO;
import com.moriba.skultem.application.dto.RevenueBreakdownDTO;
import com.moriba.skultem.application.dto.WeeklyAttendanceDTO;
import com.moriba.skultem.application.usecase.DashboardEnrollmentTrendUseCase;
import com.moriba.skultem.application.usecase.DashboardReportUseCase;
import com.moriba.skultem.application.usecase.DashboardRevenueBreakdownUseCase;
import com.moriba.skultem.application.usecase.DashboardWeeklyAttendanceReportUseCase;
import com.moriba.skultem.application.usecase.GetRecentActivitiesUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardReportUseCase dashboardReportUseCase;
    private final DashboardWeeklyAttendanceReportUseCase dashboardWeeklyAttendanceReportUseCase;
    private final DashboardRevenueBreakdownUseCase dashboardRevenueBreakdownUseCase;
    private final DashboardEnrollmentTrendUseCase dashboardEnrollmentTrendUseCase;
    private final GetRecentActivitiesUseCase getRecentActivitiesUseCase;

    @GetMapping("/admin/report")
    @PreAuthorize("@permissionService.hasSchoolRole(#schoolId, 'SCHOOL_ADMIN')")
    public ApiResponse<DashboardDTO> report(
            @AuthenticationPrincipal(expression = "activeSchoolId") String schoolId) {
        var res = dashboardReportUseCase.getDashboardSummary(schoolId);
        return new ApiResponse<>("success", 200, "Report fetch successful", res);
    }

    @GetMapping("/admin/weekly-attendance")
    @PreAuthorize("@permissionService.hasSchoolRole(#schoolId, 'SCHOOL_ADMIN')")
    public ApiResponse<List<WeeklyAttendanceDTO>> weeklyAttendance(
            @AuthenticationPrincipal(expression = "activeSchoolId") String schoolId) {
        var res = dashboardWeeklyAttendanceReportUseCase.weeklyAttendance(schoolId);
        return new ApiResponse<>("success", 200, "Report fetch successful", res);
    }

    @GetMapping("/admin/revenue")
    @PreAuthorize("@permissionService.hasSchoolRole(#schoolId, 'SCHOOL_ADMIN')")
    public ApiResponse<List<RevenueBreakdownDTO>> revenue(
            @AuthenticationPrincipal(expression = "activeSchoolId") String schoolId) {
        var res = dashboardRevenueBreakdownUseCase.getRevenueBreakdown(schoolId);
        return new ApiResponse<>("success", 200, "Report fetch successful", res);
    }

    @GetMapping("/admin/student-enrollment")
    @PreAuthorize("@permissionService.hasSchoolRole(#schoolId, 'SCHOOL_ADMIN')")
    public ApiResponse<List<MonthlyEnrollmentDTO>> studentEnrollment(
            @AuthenticationPrincipal(expression = "activeSchoolId") String schoolId) {
        var res = dashboardEnrollmentTrendUseCase.monthlyEnrollmentTrend(schoolId);
        return new ApiResponse<>("success", 200, "Report fetch successful", res);
    }

    @GetMapping("/admin/activities")
    @PreAuthorize("@permissionService.hasSchoolRole(#schoolId, 'SCHOOL_ADMIN')")
    public ApiResponse<List<ActivityDTO>> activities(
            @AuthenticationPrincipal(expression = "activeSchoolId") String schoolId,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        var res = getRecentActivitiesUseCase.execute(schoolId, size);
        return new ApiResponse<>("success", 200, "Activities fetch successful", res);
    }
}
