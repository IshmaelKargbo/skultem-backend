package com.moriba.skultem.infrastructure.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.PayslipSettingDTO;
import com.moriba.skultem.application.usecase.GetPayslipSettingUseCase;
import com.moriba.skultem.application.usecase.SavePayslipSettingUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.SavePayslipSettingDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payslip-setting")
@RequiredArgsConstructor
public class PayslipSettingController {
    private final GetPayslipSettingUseCase getPayslipSettingUseCase;
    private final SavePayslipSettingUseCase savePayslipSettingUseCase;

    // Read-only branding (logo, accent color, footer note) - opened to TEACHER too so a
    // teacher's own self-service payslip (payroll/history) renders with the school's actual
    // branding instead of falling back to blank/default styling. Saving stays admin-only.
    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<PayslipSettingDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getPayslipSettingUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Payslip settings fetched successfully", res);
    }

    @PutMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<PayslipSettingDTO> save(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SavePayslipSettingDTO param) {
        var dto = new PayslipSettingDTO(param.accentColor(), param.logoUrl(), param.footerNote(),
                param.showWatermark(), param.showAmountInWords());
        var res = savePayslipSettingUseCase.execute(school, dto);
        return new ApiResponse<>("success", 200, "Payslip settings saved successfully", res);
    }
}
