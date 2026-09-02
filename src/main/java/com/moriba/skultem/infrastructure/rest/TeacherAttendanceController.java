package com.moriba.skultem.infrastructure.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.moriba.skultem.application.dto.ClockInResponseDTO;
import com.moriba.skultem.application.dto.ClockOutResponseDTO;
import com.moriba.skultem.application.dto.MyAttendanceTodayDTO;
import com.moriba.skultem.application.dto.TeacherAttendanceDayDTO;
import com.moriba.skultem.application.dto.TeacherAttendanceDaySummaryDTO;
import com.moriba.skultem.application.dto.TeacherAttendanceRosterDTO;
import com.moriba.skultem.application.services.TeacherAttendanceService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.ClockInDTO;
import com.moriba.skultem.infrastructure.rest.dto.MarkTeacherAttendanceDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/teacher-attendance")
@RequiredArgsConstructor
@PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
public class TeacherAttendanceController {

    private final TeacherAttendanceService service;

    // Self-service - overrides the class-level admin-only restriction for just these three.
    // Any staff role can clock themselves in/out, not just teachers - actually succeeding still
    // requires a Teacher (staff/payroll) record though, see ClockInUseCase.
    @PostMapping("/clock-in")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<ClockInResponseDTO> clockIn(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody ClockInDTO param) {
        var res = service.clockIn(school, userId, param.latitude(), param.longitude());
        String message = res.alreadyClockedIn() ? "You've already clocked in today" : "Clocked in successfully";
        return new ApiResponse<>("success", 200, message, res);
    }

    @PostMapping("/clock-out")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<ClockOutResponseDTO> clockOut(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody ClockInDTO param) {
        var res = service.clockOut(school, userId, param.latitude(), param.longitude());
        String message = res.alreadyClockedOut() ? "You've already clocked out today" : "Clocked out successfully";
        return new ApiResponse<>("success", 200, message, res);
    }

    @GetMapping("/me/today")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<MyAttendanceTodayDTO> myToday(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId) {
        return new ApiResponse<>("success", 200, "Today's attendance fetched successfully",
                service.myTodayStatus(school, userId));
    }

    @PostMapping("/mark")
    public ApiResponse<Object> mark(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody MarkTeacherAttendanceDTO param) {
        service.mark(school, param.date(), param.records());
        return new ApiResponse<>("success", 200, "Attendance marked successfully", null);
    }

    // Admin clocking a teacher in/out on their behalf - for staff who can't reliably self-service
    // (internet/GPS issues, or no portal access at all). Inherits the class-level ADMIN/OWNER/
    // PROPRIETOR gate; unlike self-service clock-in/out, no location is required from the client.
    @PostMapping("/{teacherId}/admin-clock-in")
    public ApiResponse<ClockInResponseDTO> adminClockIn(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String teacherId) {
        var res = service.adminClockIn(school, teacherId);
        String message = res.alreadyClockedIn() ? "This teacher has already been clocked in today"
                : "Teacher clocked in";
        return new ApiResponse<>("success", 200, message, res);
    }

    @PostMapping("/{teacherId}/admin-clock-out")
    public ApiResponse<ClockOutResponseDTO> adminClockOut(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String teacherId) {
        var res = service.adminClockOut(school, teacherId);
        String message = res.alreadyClockedOut() ? "This teacher has already been clocked out today"
                : "Teacher clocked out";
        return new ApiResponse<>("success", 200, message, res);
    }

    @GetMapping("/teacher/{teacherId}")
    public ApiResponse<List<TeacherAttendanceDayDTO>> forTeacher(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return new ApiResponse<>("success", 200, "Attendance fetched successfully",
                service.forTeacher(school, teacherId, from, to));
    }

    @GetMapping("/roster")
    public ApiResponse<TeacherAttendanceRosterDTO> roster(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new ApiResponse<>("success", 200, "Roster fetched successfully", service.roster(school, date));
    }

    @GetMapping("/history")
    public ApiResponse<List<TeacherAttendanceDaySummaryDTO>> history(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page) {
        var res = service.history(school, page, size);
        long pages = size > 0 ? (long) Math.ceil((double) res.total() / size) : 1;
        Map<String, Object> meta = Map.of("page", page, "size", size, "count", res.total(), "pages", pages);
        return new ApiResponse<>("success", 200, "History fetched successfully", res.data(), meta);
    }

    @GetMapping("/history/{date}")
    public ApiResponse<TeacherAttendanceRosterDTO> historyDetail(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new ApiResponse<>("success", 200, "Roster fetched successfully", service.roster(school, date));
    }
}
