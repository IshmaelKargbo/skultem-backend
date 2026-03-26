package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.AuditLogDTO;
import com.moriba.skultem.application.mapper.AuditLogViewMapper;
import com.moriba.skultem.application.usecase.GetActiveAcademicYearBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListAuditLogBySchoolUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditLogController {
    private final ListAuditLogBySchoolUseCase listAuditLogBySchoolUseCase;
    private final GetActiveAcademicYearBySchoolUseCase getActiveAcademicYearBySchoolUseCase;

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR')")
    public ApiResponse<List<AuditLogDTO>> listBySchool(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {

        var yearId = academicYearId != null
                ? academicYearId
                : getActiveAcademicYearBySchoolUseCase.execute(school).getId();

        var res = listAuditLogBySchoolUseCase.execute(school, yearId, page - 1, size);
        var list = res.getContent().stream().map(AuditLogViewMapper::toDTO).toList();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Audit logs fetched successfully", list, meta);
    }
}
