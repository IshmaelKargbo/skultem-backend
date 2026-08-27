package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

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

import com.moriba.skultem.application.dto.CalendarEventDTO;
import com.moriba.skultem.application.usecase.CreateCalendarEventUseCase;
import com.moriba.skultem.application.usecase.DeleteCalendarEventUseCase;
import com.moriba.skultem.application.usecase.GetCalendarEventUseCase;
import com.moriba.skultem.application.usecase.ListCalendarEventBySchoolUseCase;
import com.moriba.skultem.application.usecase.UpdateCalendarEventUseCase;
import com.moriba.skultem.domain.model.CalendarEvent.Type;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateCalendarEventDTO;
import com.moriba.skultem.infrastructure.rest.dto.UpdateCalendarEventDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/calendar-event")
@RequiredArgsConstructor
public class CalendarEventController {
    private final CreateCalendarEventUseCase createCalendarEventUseCase;
    private final GetCalendarEventUseCase getCalendarEventUseCase;
    private final ListCalendarEventBySchoolUseCase listCalendarEventBySchoolUseCase;
    private final UpdateCalendarEventUseCase updateCalendarEventUseCase;
    private final DeleteCalendarEventUseCase deleteCalendarEventUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<CalendarEventDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody CreateCalendarEventDTO param) {
        var type = Type.valueOf(param.type());
        var res = createCalendarEventUseCase.execute(school, userId, param.title(), param.description(), type,
                param.startDate(), param.endDate(), param.location());
        return new ApiResponse<>("success", 200, "Entry added successfully", res);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'PARENT', 'ACCOUNTANT')")
    public ApiResponse<CalendarEventDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getCalendarEventUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Entry fetched successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'PARENT', 'ACCOUNTANT')")
    public ApiResponse<List<CalendarEventDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {

        var res = listCalendarEventBySchoolUseCase.execute(school, page, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Entries fetched successfully", list, meta);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<CalendarEventDTO> update(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody UpdateCalendarEventDTO param) {
        var type = Type.valueOf(param.type());
        var res = updateCalendarEventUseCase.execute(school, id, param.title(), param.description(), type,
                param.startDate(), param.endDate(), param.location());
        return new ApiResponse<>("success", 200, "Entry updated successfully", res);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        deleteCalendarEventUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Entry deleted successfully", null);
    }
}
