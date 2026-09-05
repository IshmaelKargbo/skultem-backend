package com.moriba.skultem.infrastructure.rest;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.PlatformFeeSettingDTO;
import com.moriba.skultem.application.usecase.GetPlatformFeeSettingUseCase;
import com.moriba.skultem.application.usecase.ListPlatformFeeSettingsUseCase;
import com.moriba.skultem.application.usecase.UpdatePlatformFeeSettingUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.UpdatePlatformFeeSettingDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * SYSTEM_ADMIN-only settings for the platform itself - not reachable by any school-level role
 * (ADMIN/OWNER/PROPRIETOR/ACCOUNTANT included), unlike everything under {@link FeeController}.
 * Each school has its own platform fee amount - see {@link com.moriba.skultem.domain.model.PlatformFeeSetting}.
 */
@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
@PreAuthorize("@permissionService.isSystemAdmin()")
public class PlatformSettingController {

    private final GetPlatformFeeSettingUseCase getPlatformFeeSettingUseCase;
    private final UpdatePlatformFeeSettingUseCase updatePlatformFeeSettingUseCase;
    private final ListPlatformFeeSettingsUseCase listPlatformFeeSettingsUseCase;

    @GetMapping("/fee-setting")
    public ApiResponse<List<PlatformFeeSettingDTO>> listFeeSettings() {
        var res = listPlatformFeeSettingsUseCase.execute();
        return new ApiResponse<>("success", 200, "Platform fee settings fetched successfully", res);
    }

    @GetMapping("/{schoolId}/fee-setting")
    public ApiResponse<PlatformFeeSettingDTO> getFeeSetting(@PathVariable String schoolId) {
        var res = getPlatformFeeSettingUseCase.execute(schoolId);
        return new ApiResponse<>("success", 200, "Platform fee setting fetched successfully", res);
    }

    @PutMapping("/{schoolId}/fee-setting")
    public ApiResponse<PlatformFeeSettingDTO> updateFeeSetting(@PathVariable String schoolId,
            @Valid @RequestBody UpdatePlatformFeeSettingDTO param) {
        var res = updatePlatformFeeSettingUseCase.execute(schoolId, param.amount());
        return new ApiResponse<>("success", 200, "Platform fee setting updated successfully", res);
    }
}
