package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.usecase.ActiveAcademicYearUseCase;
import com.moriba.skultem.application.usecase.CreateAcadamicYearUseCase;
import com.moriba.skultem.application.usecase.GetAcademicYearUseCase;
import com.moriba.skultem.application.usecase.ListAcademicYearUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateAcademicYearDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/academic_year")
@RequiredArgsConstructor
public class AcademicYearController {

    private final CreateAcadamicYearUseCase createAcadamicYearUseCase;
    private final ListAcademicYearUseCase listAcademicYearUseCase;
    private final GetAcademicYearUseCase getAcademicYearUseCase;
    private final ActiveAcademicYearUseCase activeAcademicYearUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<AcademicYearDTO> createAcademicYear(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateAcademicYearDTO param) {
        if (param.endDate().isBefore(param.startDate())) {
            throw new RuleException("End date must be after start date");
        }
        var res = createAcadamicYearUseCase.execute(school, param.name(), param.startDate(), param.endDate());
        return new ApiResponse<AcademicYearDTO>("success", 200, "Academic year created successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<AcademicYearDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listAcademicYearUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<List<AcademicYearDTO>>("success", 200, "Academic year fetched successfully", list, meta);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<AcademicYearDTO> setActiveAcademicYearBySchool(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = activeAcademicYearUseCase.execute(id);
        return new ApiResponse<AcademicYearDTO>("success", 200, "Academic year set active successfully", res);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<AcademicYearDTO> get(
            @PathVariable String id,
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getAcademicYearUseCase.execute(id);
        return new ApiResponse<AcademicYearDTO>("success", 200, "Academic year fetched successfully", res);
    }
}
