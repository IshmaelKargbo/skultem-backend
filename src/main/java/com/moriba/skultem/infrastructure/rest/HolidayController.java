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

import com.moriba.skultem.application.dto.HolidayDTO;
import com.moriba.skultem.application.usecase.CreateHolidayUseCase;
import com.moriba.skultem.application.usecase.DeleteHolidayUseCase;
import com.moriba.skultem.application.usecase.GetHolidayUseCase;
import com.moriba.skultem.application.usecase.ListHolidayBySchoolUseCase;
import com.moriba.skultem.application.usecase.UpdateHolidayUseCase;
import com.moriba.skultem.domain.model.Holiday.Kind;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateHolidayDTO;
import com.moriba.skultem.infrastructure.rest.dto.UpdateHolidayDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/holiday")
@RequiredArgsConstructor
public class HolidayController {
    private final CreateHolidayUseCase createHolidayUseCase;
    private final GetHolidayUseCase getHolidayUseCase;
    private final ListHolidayBySchoolUseCase listHolidayBySchoolUseCase;
    private final UpdateHolidayUseCase updateHolidayUseCase;
    private final DeleteHolidayUseCase deleteHolidayUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<HolidayDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateHolidayDTO param) {
        var kind = Kind.valueOf(param.kind());
        var res = createHolidayUseCase.execute(school, param.name(), param.date(), kind, param.fixed());
        return new ApiResponse<>("success", 200, "Holiday created successfully", res);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<HolidayDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getHolidayUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Holiday fetched successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<HolidayDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {

        var res = listHolidayBySchoolUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Holidays fetched successfully", list, meta);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<HolidayDTO> update(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody UpdateHolidayDTO param) {
        var kind = Kind.valueOf(param.kind());
        var res = updateHolidayUseCase.execute(school, id, param.name(), param.date(), kind, param.fixed());
        return new ApiResponse<>("success", 200, "Holiday updated successfully", res);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        deleteHolidayUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Holiday deleted successfully", null);
    }
}
