package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.PlatformAnnouncementDTO;
import com.moriba.skultem.application.services.PlatformAnnouncementService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.SavePlatformAnnouncementDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Platform-wide messages from the system admin, shown on every user's notice board.
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PlatformAnnouncementController {

    private final PlatformAnnouncementService service;

    // What every signed-in user sees: only the announcements that are live right now.
    @GetMapping("/announcements/active")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<PlatformAnnouncementDTO>> active() {
        return new ApiResponse<>("success", 200, "Announcements fetched successfully", service.listLive());
    }

    @GetMapping("/system/announcements")
    @PreAuthorize("@permissionService.hasPlatformPermission('COMMUNICATION')")
    public ApiResponse<List<PlatformAnnouncementDTO>> list(@RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page) {
        var res = service.list(page, size);
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());
        return new ApiResponse<>("success", 200, "Announcements fetched successfully", res.getContent(), meta);
    }

    @PostMapping("/system/announcements")
    @PreAuthorize("@permissionService.hasPlatformPermission('COMMUNICATION')")
    public ApiResponse<PlatformAnnouncementDTO> create(
            @AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody SavePlatformAnnouncementDTO param) {
        return new ApiResponse<>("success", 200, "Announcement created successfully", service.create(param, userId));
    }

    @PutMapping("/system/announcements/{id}")
    @PreAuthorize("@permissionService.hasPlatformPermission('COMMUNICATION')")
    public ApiResponse<PlatformAnnouncementDTO> update(@PathVariable String id,
            @Valid @RequestBody SavePlatformAnnouncementDTO param) {
        return new ApiResponse<>("success", 200, "Announcement updated successfully", service.update(id, param));
    }

    @DeleteMapping("/system/announcements/{id}")
    @PreAuthorize("@permissionService.hasPlatformPermission('COMMUNICATION')")
    public ApiResponse<Object> delete(@PathVariable String id) {
        service.delete(id);
        return new ApiResponse<>("success", 200, "Announcement deleted successfully", null);
    }
}
