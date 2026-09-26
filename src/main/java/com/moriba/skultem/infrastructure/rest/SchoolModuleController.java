package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.infrastructure.security.SectionNeutral;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.SchoolModuleDTO;
import com.moriba.skultem.application.usecase.DisableSchoolModuleUseCase;
import com.moriba.skultem.application.usecase.InstallSchoolModuleUseCase;
import com.moriba.skultem.application.usecase.ListSchoolModulesUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * A school's own view of the optional-module catalog - which modules exist and which it has
 * installed. Everyone in the school can read it (the menus depend on it); only the people who run
 * the school can install or disable. System admins manage any school's modules through
 * {@link PlatformSettingController}.
 */
@RestController
@RequestMapping("/api/v1/modules")
@RequiredArgsConstructor
public class SchoolModuleController {

    private final ListSchoolModulesUseCase listSchoolModulesUseCase;
    private final InstallSchoolModuleUseCase installSchoolModuleUseCase;
    private final DisableSchoolModuleUseCase disableSchoolModuleUseCase;

    @SectionNeutral
    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER', 'PARENT')")
    public ApiResponse<List<SchoolModuleDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = listSchoolModulesUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Modules fetched successfully", res);
    }

    // Installing only adds a feature, so an Admin limited to one management section may do it (the module
    // is then on for the whole school - modules are school-wide). Switching one OFF takes it away from
    // everyone, so that stays with whole-school staff: it is deliberately left unmarked (denied to a
    // section-limited caller) below.
    @SectionNeutral
    @PutMapping("/{key}/install")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<SchoolModuleDTO>> install(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String key) {
        var res = installSchoolModuleUseCase.execute(school, key, userId);
        return new ApiResponse<>("success", 200, "Module installed successfully", res);
    }

    @PutMapping("/{key}/disable")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<SchoolModuleDTO>> disable(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String key) {
        var res = disableSchoolModuleUseCase.execute(school, key, userId);
        return new ApiResponse<>("success", 200, "Module disabled successfully", res);
    }
}
