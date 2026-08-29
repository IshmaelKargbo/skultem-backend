package com.moriba.skultem.infrastructure.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.ReceiptSettingDTO;
import com.moriba.skultem.application.usecase.GetReceiptSettingUseCase;
import com.moriba.skultem.application.usecase.SaveReceiptSettingUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.SaveReceiptSettingDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/receipt-setting")
@RequiredArgsConstructor
public class ReceiptSettingController {
    private final GetReceiptSettingUseCase getReceiptSettingUseCase;
    private final SaveReceiptSettingUseCase saveReceiptSettingUseCase;

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<ReceiptSettingDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getReceiptSettingUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Receipt settings fetched successfully", res);
    }

    @PutMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<ReceiptSettingDTO> save(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SaveReceiptSettingDTO param) {
        var dto = new ReceiptSettingDTO(param.accentColor(), param.logoUrl(), param.footerNote(),
                param.showWatermark(), param.showAmountInWords());
        var res = saveReceiptSettingUseCase.execute(school, dto);
        return new ApiResponse<>("success", 200, "Receipt settings saved successfully", res);
    }
}
