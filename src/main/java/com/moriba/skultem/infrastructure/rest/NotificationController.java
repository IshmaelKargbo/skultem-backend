package com.moriba.skultem.infrastructure.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.services.NotificationService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping("/open/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER', 'PARENT')")
    public ApiResponse<Object> open(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable(required = true) String id) {
        service.openNotification(school, id);
        return new ApiResponse<>("success", 200, "Open notification successfully", null);
    }

}
