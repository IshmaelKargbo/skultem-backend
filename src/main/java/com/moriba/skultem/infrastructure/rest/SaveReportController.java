package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.SaveReportDTO;
import com.moriba.skultem.application.usecase.SaveReportUseCase;
import com.moriba.skultem.application.usecase.SaveReportUseCase.SaveReportRecord;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.application.usecase.DeleteSaveReportUseCase;
import com.moriba.skultem.application.usecase.GetSaveReportByIdUseCase;
import com.moriba.skultem.application.usecase.ListSaveReportBySchoolUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/report/save")
@RequiredArgsConstructor
public class SaveReportController {
    private final SaveReportUseCase createSaveReportUseCase;
    private final ListSaveReportBySchoolUseCase listSaveReportBySchoolUseCase;
    private final DeleteSaveReportUseCase deleteSaveReportUseCase;
    private final GetSaveReportByIdUseCase getSaveReportByIdUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<SaveReportDTO> save(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SaveReportDTO param) {
        var filters = param.filters().stream().map(
                e -> new Filter(e.field(), e.operator(), e.type(), e.value(), e.valueTo(), e.values()))
                .toList();
                
        var record = new SaveReportRecord(
                param.id(),
                param.name(),
                param.entity(),
                filters
            );
        var res = createSaveReportUseCase.execute(school, record);
        return new ApiResponse<>("success", 200, "Save report saved successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<List<SaveReportDTO>> listBySchool(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {

        var res = listSaveReportBySchoolUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Report configs fetched successfully", list, meta);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<SaveReportDTO> getById(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getSaveReportByIdUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Save report fetched successfully", res);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<Object> delete(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        deleteSaveReportUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Save report deleted successfully", null);
    }
}
