package com.moriba.skultem.infrastructure.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.dto.SystemAdminStatsDTO;
import com.moriba.skultem.application.usecase.SetSchoolStatusUseCase;
import com.moriba.skultem.application.usecase.SystemAdminStatsUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemAdminController {
    private final SystemAdminStatsUseCase statsUseCase;
    private final SetSchoolStatusUseCase setSchoolStatusUseCase;

    @GetMapping("/stats")
    @PreAuthorize("@permissionService.isSystemAdmin()")
    public ApiResponse<SystemAdminStatsDTO> stats() {
        var res = statsUseCase.execute();
        return new ApiResponse<SystemAdminStatsDTO>("success", 200, "System stats fetched successfully", res);
    }

    @PutMapping("/school/{id}/status")
    @PreAuthorize("@permissionService.isSystemAdmin()")
    public ApiResponse<SchoolDTO> updateSchoolStatus(@PathVariable("id") String schoolId,
            @RequestParam("status") String status) {
        var res = setSchoolStatusUseCase.execute(schoolId, status);
        return new ApiResponse<SchoolDTO>("success", 200, "School status updated successfully", res);
    }
}
