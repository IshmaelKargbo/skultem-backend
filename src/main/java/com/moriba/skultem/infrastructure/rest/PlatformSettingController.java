package com.moriba.skultem.infrastructure.rest;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.NationalCalendarDTO;
import com.moriba.skultem.application.dto.PlatformFeeSettingDTO;
import com.moriba.skultem.application.dto.SchoolModuleDTO;
import com.moriba.skultem.application.usecase.DisableSchoolModuleUseCase;
import com.moriba.skultem.application.usecase.InstallSchoolModuleUseCase;
import com.moriba.skultem.application.usecase.ListSchoolModulesUseCase;
import com.moriba.skultem.application.usecase.GetNationalCalendarUseCase;
import com.moriba.skultem.application.usecase.SaveNationalCalendarUseCase;
import com.moriba.skultem.infrastructure.rest.dto.SaveNationalCalendarDTO;
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
    private final GetNationalCalendarUseCase getNationalCalendarUseCase;
    private final SaveNationalCalendarUseCase saveNationalCalendarUseCase;
    private final ListSchoolModulesUseCase listSchoolModulesUseCase;
    private final InstallSchoolModuleUseCase installSchoolModuleUseCase;
    private final DisableSchoolModuleUseCase disableSchoolModuleUseCase;

    // A system admin managing any school's optional modules - same rules as the school's own
    // Modules page (SchoolModuleController), just for a school picked by id.
    @GetMapping("/{schoolId}/modules")
    public ApiResponse<List<SchoolModuleDTO>> listSchoolModules(@PathVariable String schoolId) {
        var res = listSchoolModulesUseCase.execute(schoolId);
        return new ApiResponse<>("success", 200, "Modules fetched successfully", res);
    }

    @PutMapping("/{schoolId}/modules/{key}/install")
    public ApiResponse<List<SchoolModuleDTO>> installSchoolModule(@PathVariable String schoolId,
            @PathVariable String key, @AuthenticationPrincipal(expression = "userId") String userId) {
        var res = installSchoolModuleUseCase.execute(schoolId, key, userId);
        return new ApiResponse<>("success", 200, "Module installed successfully", res);
    }

    @PutMapping("/{schoolId}/modules/{key}/disable")
    public ApiResponse<List<SchoolModuleDTO>> disableSchoolModule(@PathVariable String schoolId,
            @PathVariable String key, @AuthenticationPrincipal(expression = "userId") String userId) {
        var res = disableSchoolModuleUseCase.execute(schoolId, key, userId);
        return new ApiResponse<>("success", 200, "Module disabled successfully", res);
    }

    // The platform-wide (ministry) academic year + terms every newly-created school starts from -
    // see NationalAcademicYear. data is null until one has been configured.
    @GetMapping("/calendar")
    public ApiResponse<NationalCalendarDTO> getCalendar() {
        var res = getNationalCalendarUseCase.execute();
        return new ApiResponse<>("success", 200, "National calendar fetched successfully", res);
    }

    @PutMapping("/calendar")
    public ApiResponse<NationalCalendarDTO> saveCalendar(@Valid @RequestBody SaveNationalCalendarDTO param) {
        var res = saveNationalCalendarUseCase.execute(param);
        return new ApiResponse<>("success", 200, "National calendar saved successfully", res);
    }

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
