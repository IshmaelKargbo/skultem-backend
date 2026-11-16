package com.moriba.skultem.infrastructure.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.WeeklyPlanService;
import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.security.RequiresModule;

import lombok.RequiredArgsConstructor;

/**
 * Weekly plans (WeeklyPlanService): a class section's school term as weeks, a few subjects each school day. Leadership
 * builds and publishes them (the same roles as the timetable), teachers can look, and a parent sees their own
 * child's published plan.
 */
@RestController
@RequiresModule(FeatureModule.TIMETABLE)
@RequestMapping("/api/v1/weekly-plans")
@RequiredArgsConstructor
public class WeeklyPlanController {

    private static final String EDIT = "@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PRINCIPAL', 'SUPER_ADMIN', 'OWNER', 'PROPRIETOR')";
    private static final String VIEW = "@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PRINCIPAL', 'SUPER_ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')";

    private final WeeklyPlanService plans;

    public record Slots(List<WeeklyPlanService.Slot> slots) {
    }

    public record Entries(List<WeeklyPlanService.EntryInput> entries) {
    }

    public record Cycle(int cycleWeeks, List<WeeklyPlanService.CycleInput> entries) {
    }

    public record Status(boolean published) {
    }

    @GetMapping("/slots")
    @PreAuthorize(VIEW)
    public ApiResponse<List<WeeklyPlanService.Slot>> slots(@AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return new ApiResponse<>("success", 200, "Sessions", plans.slots(school));
    }

    @PutMapping("/slots")
    @PreAuthorize(EDIT)
    public ApiResponse<List<WeeklyPlanService.Slot>> saveSlots(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @RequestBody Slots body) {
        return new ApiResponse<>("success", 200, "Sessions saved", plans.saveSlots(school, body.slots()));
    }

    // A class's own sessions (for every section of it), or back to the school's.
    @GetMapping("/slots/class")
    @PreAuthorize(VIEW)
    public ApiResponse<WeeklyPlanService.ClassSlots> classSlots(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @RequestParam String classSessionId) {
        return new ApiResponse<>("success", 200, "Sessions", plans.slotsFor(school, classSessionId));
    }

    @PutMapping("/slots/class")
    @PreAuthorize(EDIT)
    public ApiResponse<WeeklyPlanService.ClassSlots> saveClassSlots(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @RequestParam String classSessionId,
            @RequestBody Slots body) {
        return new ApiResponse<>("success", 200, "Class sessions saved", plans.saveClassSlots(school, classSessionId, body.slots()));
    }

    @DeleteMapping("/slots/class")
    @PreAuthorize(EDIT)
    public ApiResponse<WeeklyPlanService.ClassSlots> clearClassSlots(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @RequestParam String classSessionId) {
        return new ApiResponse<>("success", 200, "Using the school's sessions", plans.clearClassSlots(school, classSessionId));
    }

    @GetMapping
    @PreAuthorize(VIEW)
    public ApiResponse<WeeklyPlanService.Plan> plan(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String classSessionId, @RequestParam(required = false) String termId) {
        return new ApiResponse<>("success", 200, "Weekly plan", plans.plan(school, classSessionId, termId));
    }

    @PostMapping("/auto-fill")
    @PreAuthorize(EDIT)
    public ApiResponse<WeeklyPlanService.Plan> autoFill(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String classSessionId, @RequestParam(required = false) String termId,
            @RequestParam(defaultValue = "0") int cycleWeeks) {
        return new ApiResponse<>("success", 200, "Weekly plan filled",
                plans.autoFill(school, classSessionId, termId, cycleWeeks));
    }

    // A repeating plan: a few weeks by weekday (e.g. a 4-week month), spread over the whole term.
    @PutMapping("/cycle")
    @PreAuthorize(EDIT)
    public ApiResponse<WeeklyPlanService.Plan> saveCycle(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String classSessionId, @RequestParam(required = false) String termId, @RequestBody Cycle body) {
        return new ApiResponse<>("success", 200, "Weekly plan saved",
                plans.saveCycle(school, classSessionId, termId, body.cycleWeeks(), body.entries()));
    }

    @PutMapping
    @PreAuthorize(EDIT)
    public ApiResponse<WeeklyPlanService.Plan> save(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String classSessionId, @RequestParam(required = false) String termId, @RequestBody Entries body) {
        return new ApiResponse<>("success", 200, "Weekly plan saved", plans.save(school, classSessionId, termId, body.entries()));
    }

    @PatchMapping("/status")
    @PreAuthorize(EDIT)
    public ApiResponse<WeeklyPlanService.Plan> status(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String classSessionId, @RequestParam(required = false) String termId, @RequestBody Status body) {
        return new ApiResponse<>("success", 200, body.published() ? "Weekly plan published" : "Weekly plan unpublished",
                plans.setStatus(school, classSessionId, termId, body.published()));
    }

    // A parent's own child: this week and next, as far as the school has published it.
    @GetMapping("/child/{studentId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'PARENT')")
    public ApiResponse<Map<String, Object>> child(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId, @PathVariable String studentId,
            @RequestParam(required = false) String from) {
        if (!plans.isParentOf(userId, studentId, school)) throw new NotFoundException("Student not found");
        LocalDate start = from == null || from.isBlank() ? WeeklyPlanService.today() : LocalDate.parse(from);
        return new ApiResponse<>("success", 200, "Weekly plan", plans.forStudent(school, studentId, start).orElse(null));
    }
}
