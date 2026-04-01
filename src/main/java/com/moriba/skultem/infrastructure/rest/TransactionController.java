package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.TermDTO;
import com.moriba.skultem.application.usecase.CreateTermUseCase;
import com.moriba.skultem.application.usecase.GetActiveTermUseCase;
import com.moriba.skultem.application.usecase.ActivateTermUseCase;
import com.moriba.skultem.application.usecase.ListTermByAcademicYearIdUseCase;
import com.moriba.skultem.application.usecase.ListTermBySchoolIdUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateTermDTO;

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
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final CreateTermUseCase createTermUseCase;
    private final ActivateTermUseCase activateTermUseCase;
    private final GetActiveTermUseCase getActiveTermUseCase;
    private final ListTermBySchoolIdUseCase listTermBySchoolIdUseCase;
    private final ListTermByAcademicYearIdUseCase listTermByAcademicYearIdUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR')")
    public ApiResponse<TermDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateTermDTO param) {
        var res = createTermUseCase.execute(school, param.name(), param.startDate(), param.endDate());
        return new ApiResponse<>("success", 200, "Term created successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<List<TermDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page) {
        var res = listTermBySchoolIdUseCase.execute(school, page - 1, size);
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());
        return new ApiResponse<>("success", 200, "Terms fetched successfully", res.getContent(), meta);
    }

    @GetMapping("/active")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'TEACHER', 'ACCOUNTANT', 'PARENT')")
    public ApiResponse<TermDTO> getActive(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getActiveTermUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Get active term successfully", res);
    }

    @GetMapping("/academic-year/{academicYearId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT', 'PARENT', 'TEACHER')")
    public ApiResponse<List<TermDTO>> listByAcademicYear(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String academicYearId,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page) {
        var res = listTermByAcademicYearIdUseCase.execute(school, academicYearId, page - 1, size);
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());
        return new ApiResponse<>("success", 200, "Terms fetched successfully", res.getContent(), meta);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'ADMIN', 'PROPRIETOR')")
    public ApiResponse<TermDTO> activate(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = activateTermUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Term activated successfully", res);
    }

}