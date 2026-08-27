package com.moriba.skultem.infrastructure.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.IdCardSettingDTO;
import com.moriba.skultem.application.usecase.GetIdCardSettingUseCase;
import com.moriba.skultem.application.usecase.SaveIdCardSettingUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.SaveIdCardSettingDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/id-card-setting")
@RequiredArgsConstructor
public class IdCardSettingController {
    private final GetIdCardSettingUseCase getIdCardSettingUseCase;
    private final SaveIdCardSettingUseCase saveIdCardSettingUseCase;

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<IdCardSettingDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getIdCardSettingUseCase.execute(school);
        return new ApiResponse<>("success", 200, "ID card settings fetched successfully", res);
    }

    @PutMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<IdCardSettingDTO> save(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SaveIdCardSettingDTO param) {
        var dto = new IdCardSettingDTO(param.layout(), param.profileShape(), param.headerColor(),
                param.footerColor(), param.headerTextColor(), param.primaryTextColor(), param.widthMm(),
                param.heightMm(), param.bgImageUrl(), param.bgOpacity(), param.schoolName(), param.schoolAddress(),
                param.principalName(), param.fields(), param.validityYears());
        var res = saveIdCardSettingUseCase.execute(school, dto);
        return new ApiResponse<>("success", 200, "ID card settings saved successfully", res);
    }
}
