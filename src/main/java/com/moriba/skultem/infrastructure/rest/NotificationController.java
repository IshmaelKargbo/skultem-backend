package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.NotificationDTO;
import com.moriba.skultem.application.services.NotificationService;
import com.moriba.skultem.application.usecase.ListNotificationByParentUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;
    private final ListNotificationByParentUseCase listNotificationByParentUseCase;

    // Despite the use case's name, notifications aren't parent-specific at the
    // data level — `owner` is any User — so this is open to every portal role.
    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER', 'PARENT')")
    public ApiResponse<List<NotificationDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listNotificationByParentUseCase.execute(school, userId, page, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Notifications fetched successfully", list, meta);
    }

    @GetMapping("/open/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER', 'PARENT')")
    public ApiResponse<Object> open(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable(required = true) String id) {
        service.openNotification(school, id);
        return new ApiResponse<>("success", 200, "Open notification successfully", null);
    }

}
