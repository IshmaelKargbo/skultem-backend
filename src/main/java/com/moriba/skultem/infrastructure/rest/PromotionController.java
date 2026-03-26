package com.moriba.skultem.infrastructure.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.usecase.PromoteClassUseCase;
import com.moriba.skultem.application.usecase.PromoteSchoolUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/promotion")
@RequiredArgsConstructor
public class PromotionController {
    private final PromoteClassUseCase promoteClassUseCase;
    private final PromoteSchoolUseCase promoteSchoolUseCase;

    @PostMapping("/class/{classId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR')")
    public ApiResponse<Object> promoteClass(@PathVariable String classId, @RequestParam String school) {
        promoteClassUseCase.execute(school, classId);
        return new ApiResponse<Object>("success", 200, "Class promoted successfully", null);
    }

    @PostMapping("/school")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR')")
    public ApiResponse<Object> promoteSchool(@RequestParam String school) {
        promoteSchoolUseCase.execute(school);
        return new ApiResponse<Object>("success", 200, "School promoted successfully", null);
    }
}
