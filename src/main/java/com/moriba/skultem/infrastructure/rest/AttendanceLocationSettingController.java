package com.moriba.skultem.infrastructure.rest;

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

@RestController
@RequestMapping("/api/v1/attendance-location")
@RequiredArgsConstructor
@PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
public class AttendanceLocationSettingController {
    private final GetAttendanceLocationSettingUseCase getAttendanceLocationSettingUseCase;
    private final SaveAttendanceLocationSettingUseCase saveAttendanceLocationSettingUseCase;

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
}
