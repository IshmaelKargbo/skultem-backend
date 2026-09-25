package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.infrastructure.security.RequiresModule;
import com.moriba.skultem.infrastructure.security.SectionScoped;
import com.moriba.skultem.application.services.SectionScopeService;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.AttendanceLocationSettingDTO;
import com.moriba.skultem.application.usecase.GetAttendanceLocationSettingUseCase;
import com.moriba.skultem.application.usecase.SaveAttendanceLocationSettingUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.SaveAttendanceLocationSettingDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiresModule(FeatureModule.STAFF_HR)
@RestController
@RequestMapping("/api/v1/attendance-location")
@RequiredArgsConstructor
@PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
public class AttendanceLocationSettingController {
    private final GetAttendanceLocationSettingUseCase getAttendanceLocationSettingUseCase;
    private final SaveAttendanceLocationSettingUseCase saveAttendanceLocationSettingUseCase;
    private final SectionScopeService sectionScopeService;

    @GetMapping
    public ApiResponse<AttendanceLocationSettingDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getAttendanceLocationSettingUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Attendance location settings fetched successfully", res);
    }

    @PutMapping
    public ApiResponse<AttendanceLocationSettingDTO> save(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SaveAttendanceLocationSettingDTO param) {
        var res = saveAttendanceLocationSettingUseCase.execute(school, param.latitude(), param.longitude(),
                param.radiusMeters(), param.allowedIps());
        return new ApiResponse<>("success", 200, "Attendance location settings saved successfully", res);
    }

    // Locations configured per management section. Whole-school callers get every section's; a
    // section-limited Admin only their own section(s)'.
    @SectionScoped
    @GetMapping("/sections")
    public ApiResponse<List<AttendanceLocationSettingDTO>> sections(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var scope = sectionScopeService.current();
        var res = getAttendanceLocationSettingUseCase.executeForSections(school,
                scope.wholeSchool() ? null : scope.sectionIds());
        return new ApiResponse<>("success", 200, "Section attendance locations fetched successfully", res);
    }

    @SectionScoped
    @PutMapping("/sections/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.managementSection(#id)")
    public ApiResponse<AttendanceLocationSettingDTO> saveForSection(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody SaveAttendanceLocationSettingDTO param) {
        var res = saveAttendanceLocationSettingUseCase.executeForSection(school, id, param.latitude(),
                param.longitude(), param.radiusMeters(), param.allowedIps());
        return new ApiResponse<>("success", 200, "Section attendance location saved successfully", res);
    }
}
