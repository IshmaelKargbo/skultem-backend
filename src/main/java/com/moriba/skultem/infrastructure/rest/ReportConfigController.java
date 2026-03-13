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

import com.moriba.skultem.application.dto.ReportConfigDTO;
import com.moriba.skultem.application.usecase.CreateReportConfigUseCase;
import com.moriba.skultem.application.usecase.CreateReportConfigUseCase.CreateReportConfigRecord;
import com.moriba.skultem.application.usecase.DeleteReportConfigUseCase;
import com.moriba.skultem.application.usecase.GetReportConfigByIdUseCase;
import com.moriba.skultem.application.usecase.ListReportConfigBySchoolUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateReportConfigDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/report/config")
@RequiredArgsConstructor
public class ReportConfigController {
    private final CreateReportConfigUseCase createReportConfigUseCase;
    private final ListReportConfigBySchoolUseCase listReportConfigBySchoolUseCase;
    private final DeleteReportConfigUseCase deleteReportConfigUseCase;
    private final GetReportConfigByIdUseCase getReportConfigByIdUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<ReportConfigDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateReportConfigDTO param) {
        var record = new CreateReportConfigRecord(
                school,
                param.name(),
                param.type(),
                param.format(),
                param.classId(),
                param.classSessionId(),
                param.teacherSubjectId(),
                param.termId(),
                param.startDate(),
                param.endDate());
        var res = createReportConfigUseCase.execute(record);
        return new ApiResponse<>("success", 200, "Report config saved successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<List<ReportConfigDTO>> listBySchool(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {

        var res = listReportConfigBySchoolUseCase.execute(school, page - 1, size);
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
    public ApiResponse<ReportConfigDTO> getById(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getReportConfigByIdUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Report config fetched successfully", res);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<Object> delete(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        deleteReportConfigUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Report config deleted successfully", null);
    }
}
