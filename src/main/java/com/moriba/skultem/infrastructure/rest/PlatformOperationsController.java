package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.services.PlatformOperationsService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

// How the platform is doing, and the notifications that have gone out. Read-only.
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class PlatformOperationsController {

    private static final java.util.Set<Integer> PERIODS = java.util.Set.of(7, 30, 90);

    private final PlatformOperationsService operations;

    @GetMapping("/health")
    @PreAuthorize("@permissionService.hasPlatformPermission('HEALTH')")
    public ApiResponse<PlatformOperationsService.Health> health() {
        return new ApiResponse<>("success", 200, "Health checked successfully", operations.health());
    }

    @GetMapping("/notifications/overview")
    @PreAuthorize("@permissionService.hasPlatformPermission('COMMUNICATION')")
    public ApiResponse<PlatformOperationsService.Overview> overview(@RequestParam(defaultValue = "30") int days) {
        return new ApiResponse<>("success", 200, "Notification overview fetched successfully",
                operations.overview(PERIODS.contains(days) ? days : 30));
    }

    @GetMapping("/notifications/broadcasts")
    @PreAuthorize("@permissionService.hasPlatformPermission('COMMUNICATION')")
    public ApiResponse<List<PlatformOperationsService.Broadcast>> broadcasts(
            @RequestParam(required = false) String search, @RequestParam(required = false) String schoolId,
            @RequestParam(defaultValue = "10") Integer size, @RequestParam(defaultValue = "1") Integer page) {
        var res = operations.broadcasts(search, schoolId, page, Math.min(size, 100));
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());
        return new ApiResponse<>("success", 200, "Broadcasts fetched successfully", res.getContent(), meta);
    }
}
