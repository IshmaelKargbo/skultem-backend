package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.application.dto.PeriodDTO;
import com.moriba.skultem.application.dto.RoomDTO;
import com.moriba.skultem.application.dto.TimingDTO;
import com.moriba.skultem.application.dto.WorkingDayDTO;
import com.moriba.skultem.application.services.TimetableService;
import com.moriba.skultem.application.usecase.SetWorkingDayUseCase;
import com.moriba.skultem.domain.model.WorkingDay;
import com.moriba.skultem.infrastructure.rest.dto.*;
import com.moriba.skultem.infrastructure.rest.mapper.MetaMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableSvc;

    @PostMapping("/room")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<RoomDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateRoomDTO param) {
        var res = timetableSvc.createRoom(school, param.name(), param.no(), param.description());
        return new ApiResponse<>("success", 200, "Room created successfully", res);
    }

    @GetMapping("/room")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<RoomDTO>> roomSearch(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) String search) {
        var res = timetableSvc.searchRoom(school, search, page - 1, size);
        Map<String, Object> meta = MetaMapper.toMeta(res);
        return new ApiResponse<>("success", 200, "Room fetched successfully", res.getContent(), meta);
    }

    @PostMapping("/timing")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<TimingDTO> setTiming(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateTimingDTO param) {
        var res = timetableSvc.setTiming(school, param.startTime(), param.endTime(), param.periodDuration(), param.breakDuration(), param.lunchDuration());
        return new ApiResponse<>("success", 200, "School timing set successfully", res);
    }

    @GetMapping("/timing")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<TimingDTO> getTiming(@AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = timetableSvc.getTiming(school);
        return new ApiResponse<>("success", 200, "timing fetched successfully", res);
    }

    @GetMapping("/working-day")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<WorkingDayDTO>> listWorkingDays(@AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = timetableSvc.listWorkingDays(school);
        return new ApiResponse<>("success", 200, "Working days fetch successfully", res);
    }

    @PostMapping("/working-day")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<WorkingDayDTO>> setWorkingDays(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
                                                           @Valid @RequestBody CreateWorkingDayDTO param) {
        List<SetWorkingDayUseCase.WorkingDayRecord> days = param.days().stream().map((e) -> new SetWorkingDayUseCase.WorkingDayRecord(WorkingDay.Day.valueOf(e.day()), e.state())).toList();
        var res = timetableSvc.setWorkingDays(school, days);
        return new ApiResponse<>("success", 200, "Working days set successfully", res);
    }

    @GetMapping("/period/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<PeriodDTO>> getTimetable(@AuthenticationPrincipal(expression = "activeSchoolId") String school, @Valid @PathVariable String id) {
        var res = timetableSvc.getTimeTable(id);
        return new ApiResponse<>("success", 200, "timetable fetch successfully", res);
    }

    @PostMapping("/period")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<PeriodDTO> addPeriod(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
                                                  @Valid @RequestBody CreatePeriodDTO param) {
        var res = timetableSvc.createPeriod(school, param.session());
        return new ApiResponse<>("success", 200, "Period added successfully", res);
    }

    @PostMapping("/break")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<PeriodDTO> addBreak(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
                                            @Valid @RequestBody CreatePeriodDTO param) {
        var res = timetableSvc.createBreak(school, param.session());
        return new ApiResponse<>("success", 200, "Break added successfully", res);
    }

    @PostMapping("/lunch")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<PeriodDTO> addLunch(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
                                            @Valid @RequestBody CreatePeriodDTO param) {
        var res = timetableSvc.createLunch(school, param.session());
        return new ApiResponse<>("success", 200, "Lunch added successfully", res);
    }
}