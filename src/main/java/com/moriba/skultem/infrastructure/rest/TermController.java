package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.TermDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.usecase.CreateTermUseCase;
import com.moriba.skultem.application.usecase.ListTermByAcademicYearIdUseCase;
import com.moriba.skultem.application.usecase.ListTermBySchoolIdUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateTermDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/term")
@RequiredArgsConstructor
public class TermController {

    private final CreateTermUseCase createTermUseCase;
    private final ListTermBySchoolIdUseCase listTermBySchoolIdUseCase;
    private final ListTermByAcademicYearIdUseCase listTermAcademicYearIdUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<TermDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateTermDTO param) {
        if (param.endDate().isBefore(param.startDate())) {
            throw new RuleException("End date must be after start date");
        }
        var res = createTermUseCase.execute(school, param.academicYearId(), param.name(), param.startDate(),
                param.endDate());
        return new ApiResponse<TermDTO>("success", 200, "Term created successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<TermDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listTermBySchoolIdUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<List<TermDTO>>("success", 200, "Terms fetched successfully", list, meta);
    }

    @GetMapping("/academic-year/{academicYearId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<TermDTO>> listByAcademicYear(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String academicYearId,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listTermAcademicYearIdUseCase.execute(school, academicYearId, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<List<TermDTO>>("success", 200, "Terms fetched successfully", list, meta);
    }
}
