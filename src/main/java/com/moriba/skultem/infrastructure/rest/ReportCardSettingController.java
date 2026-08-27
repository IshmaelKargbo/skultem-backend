package com.moriba.skultem.infrastructure.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.ReportCardSettingDTO;
import com.moriba.skultem.application.usecase.GetReportCardSettingUseCase;
import com.moriba.skultem.application.usecase.SaveReportCardSettingUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.SaveReportCardSettingDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/report-card-setting")
@RequiredArgsConstructor
public class ReportCardSettingController {
    private final GetReportCardSettingUseCase getReportCardSettingUseCase;
    private final SaveReportCardSettingUseCase saveReportCardSettingUseCase;

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<ReportCardSettingDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getReportCardSettingUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Report card settings fetched successfully", res);
    }

    @PutMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<ReportCardSettingDTO> save(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SaveReportCardSettingDTO param) {
        var dto = new ReportCardSettingDTO(param.headerColor(), param.logoUrl(), param.footerNote(),
                param.showAttendance(), param.showRemarks(), param.showPosition(), param.showSignatures(),
                param.showGradeScale());
        var res = saveReportCardSettingUseCase.execute(school, dto);
        return new ApiResponse<>("success", 200, "Report card settings saved successfully", res);
    }
}
