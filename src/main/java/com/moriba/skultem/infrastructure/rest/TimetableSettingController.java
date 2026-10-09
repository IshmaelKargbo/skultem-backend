package com.moriba.skultem.infrastructure.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.TimetableSettingDTO;
import com.moriba.skultem.application.usecase.GetTimetableSettingUseCase;
import com.moriba.skultem.application.usecase.SaveTimetableSettingUseCase;
import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.SaveTimetableSettingDTO;
import com.moriba.skultem.infrastructure.security.RequiresModule;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Design of the downloadable timetable PDF. Everyone who can download a timetable (admins, teachers,
// parents) reads it; only management saves it.
@RequiresModule(FeatureModule.TIMETABLE)
@RestController
@RequestMapping("/api/v1/timetable-setting")
@RequiredArgsConstructor
public class TimetableSettingController {
    private final GetTimetableSettingUseCase getTimetableSettingUseCase;
    private final SaveTimetableSettingUseCase saveTimetableSettingUseCase;

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PRINCIPAL', 'SUPER_ADMIN', 'PROPRIETOR', 'TEACHER', 'PARENT')")
    public ApiResponse<TimetableSettingDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getTimetableSettingUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Timetable settings fetched successfully", res);
    }

    @PutMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PRINCIPAL', 'SUPER_ADMIN', 'PROPRIETOR')")
    public ApiResponse<TimetableSettingDTO> save(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SaveTimetableSettingDTO param) {
        var dto = new TimetableSettingDTO(param.title(), param.accentColor(), param.orientation(),
                param.showLogo(), param.showIcons(), param.showTeacher(), param.showRoom(),
                param.showPeriodTimes(), param.footerNote());
        var res = saveTimetableSettingUseCase.execute(school, dto);
        return new ApiResponse<>("success", 200, "Timetable settings saved successfully", res);
    }
}
