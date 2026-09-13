package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.application.dto.PeriodDTO;
import com.moriba.skultem.application.dto.RoomDTO;
import com.moriba.skultem.application.dto.TimetableDTO;
import com.moriba.skultem.application.dto.TimingDTO;
import com.moriba.skultem.application.dto.TimingLevelDTO;
import com.moriba.skultem.application.dto.WorkingDayDTO;
import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.services.TimetableService;
import com.moriba.skultem.application.usecase.SetWorkingDayUseCase;
import com.moriba.skultem.domain.model.WorkingDay;
import com.moriba.skultem.domain.model.WorkingDay.Day;
import com.moriba.skultem.domain.vo.Level;
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

    @PostMapping()
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<TimetableDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateTimetableDTO param) {
        var res = timetableSvc.createTimetable(school, param.period(), param.subject(), Day.valueOf(param.day()),
                param.room(), param.color());
        return new ApiResponse<>("success", 200, "Timetable created successfully", res);
    }

    @PostMapping("/room")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<RoomDTO> createRoom(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateRoomDTO param) {
        var res = timetableSvc.createRoom(school, param.name(), param.no(), param.description());
        return new ApiResponse<>("success", 200, "Room created successfully", res);
    }

    @PatchMapping("/room")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<RoomDTO> updateRoom(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody EditRoomDTO param) {
        var res = timetableSvc.updateRoom(param.id(), school, param.name(), param.no(), param.description());
        return new ApiResponse<>("success", 200, "Room updated successfully", res);
    }

    @GetMapping("/room")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'PARENT')")
    public ApiResponse<List<RoomDTO>> roomSearch(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) String search) {
        var res = timetableSvc.searchRoom(school, search, page - 1, size);
        Map<String, Object> meta = MetaMapper.toMeta(res);
        return new ApiResponse<>("success", 200, "Room fetched successfully", res.getContent(), meta);
    }

    // Timing templates - a school can have several (e.g. "Default", "Primary", "JSS/SSS"), each
    // assignable to one or more Levels. See ResolveTimingForLevelUseCase for how a class session
    // picks which one applies.
    @GetMapping("/timing")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<TimingDTO>> listTimings(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = timetableSvc.listTimings(school);
        return new ApiResponse<>("success", 200, "Timing templates fetched successfully", res);
    }

    @PostMapping("/timing")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<TimingDTO> createTiming(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateTimingDTO param) {
        var res = timetableSvc.saveTiming(school, null, param.name(), param.startTime(), param.endTime(),
                param.periodDuration(), param.breakDuration(), param.lunchDuration());
        return new ApiResponse<>("success", 200, "Timing template created successfully", res);
    }

    @PatchMapping("/timing/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<TimingDTO> updateTiming(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody CreateTimingDTO param) {
        var res = timetableSvc.saveTiming(school, id, param.name(), param.startTime(), param.endTime(),
                param.periodDuration(), param.breakDuration(), param.lunchDuration());
        return new ApiResponse<>("success", 200, "Timing template updated successfully", res);
    }

    @PatchMapping("/timing/{id}/default")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<TimingDTO> setDefaultTiming(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = timetableSvc.setDefaultTiming(school, id);
        return new ApiResponse<>("success", 200, "Default timing template set successfully", res);
    }

    @DeleteMapping("/timing/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<TimingDTO> deleteTiming(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = timetableSvc.deleteTiming(school, id);
        return new ApiResponse<>("success", 200, "Timing template deleted successfully", res);
    }

    @GetMapping("/timing/level")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<TimingLevelDTO>> listTimingLevels(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = timetableSvc.listTimingLevels(school);
        return new ApiResponse<>("success", 200, "Timing level assignments fetched successfully", res);
    }

    @PostMapping("/timing/level")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<TimingLevelDTO> assignTimingLevel(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody AssignTimingLevelDTO param) {
        var res = timetableSvc.assignTimingLevel(school, Level.valueOf(param.level()), param.timingId());
        return new ApiResponse<>("success", 200, "Timing template assigned successfully", res);
    }

    @GetMapping("/working-day")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'PARENT')")
    public ApiResponse<List<WorkingDayDTO>> listWorkingDays(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            // A class session's timetable grid resolves its days via `session` (its Level's
            // template); the Settings page's per-template editor passes `timingId` directly.
            @RequestParam(required = false) String session,
            @RequestParam(required = false) String timingId) {
        if (session == null && timingId == null) {
            throw new BadRequestException("Either session or timingId is required");
        }

        var res = session != null
                ? timetableSvc.listWorkingDaysBySession(school, session)
                : timetableSvc.listWorkingDaysByTiming(school, timingId);

        return new ApiResponse<>("success", 200, "Working days fetch successfully", res);
    }

    @PostMapping("/working-day")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<WorkingDayDTO>> setWorkingDays(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateWorkingDayDTO param) {
        List<SetWorkingDayUseCase.WorkingDayRecord> days = param.days().stream()
                .map((e) -> new SetWorkingDayUseCase.WorkingDayRecord(WorkingDay.Day.valueOf(e.day()), e.state()))
                .toList();
        var res = timetableSvc.setWorkingDays(school, param.timingId(), days);
        return new ApiResponse<>("success", 200, "Working days set successfully", res);
    }

    @GetMapping("/period/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'PARENT')")
    public ApiResponse<List<PeriodDTO>> getTimetable(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @Valid @PathVariable String id) {
        var res = timetableSvc.getTimeTable(id);
        return new ApiResponse<>("success", 200, "timetable fetch successfully", res);
    }

    @DeleteMapping("/period/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<PeriodDTO> deletePeriod(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @Valid @PathVariable String id) {
        var res = timetableSvc.deletePeriod(id);
        return new ApiResponse<>("success", 200, "period delete successfully", res);
    }

    @DeleteMapping("/room/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<RoomDTO> deleteRoom(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @Valid @PathVariable String id) {
        var res = timetableSvc.deleteRoom(school, id);
        return new ApiResponse<>("success", 200, "room delete successfully", res);
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
