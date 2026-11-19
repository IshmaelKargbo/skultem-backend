package com.moriba.skultem.infrastructure.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.domain.model.MobileDevice;
import com.moriba.skultem.domain.repository.MobileDeviceRepository;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.security.SectionNeutral;

import lombok.RequiredArgsConstructor;

// The mobile app registers its Firebase token here after sign-in (and whenever Firebase issues a new one), and
// removes it on sign-out. Every school role can use it, for themselves.
@RestController
@RequestMapping("/api/v1/push/devices")
@RequiredArgsConstructor
public class MobileDeviceController {

    private final MobileDeviceRepository repo;

    public record RegisterDTO(String token, String platform) {
    }

    public record UnregisterDTO(String token) {
    }

    @SectionNeutral
    @PostMapping
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<Void> register(
            @AuthenticationPrincipal(expression = "userId") String userId,
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestBody RegisterDTO body) {
        if (body == null || body.token() == null || body.token().isBlank() || body.token().length() > 4096) {
            throw new BadRequestException("That isn't a valid device token");
        }
        MobileDevice.Platform platform;
        try {
            platform = MobileDevice.Platform.valueOf(String.valueOf(body.platform()).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Platform must be ANDROID or IOS");
        }
        String token = body.token().trim();
        var existing = repo.findByToken(token).orElse(null);
        if (existing != null && !existing.getUserId().equals(userId)) {
            // Someone else signed in on this phone: it follows the new user.
            repo.deleteByToken(token);
            existing = null;
        }
        if (existing == null) {
            repo.save(MobileDevice.create(userId, school, token, platform));
        } else {
            existing.refresh(school, platform);
            repo.save(existing);
        }
        return new ApiResponse<>("success", 200, "Notifications turned on for this phone", null);
    }

    @SectionNeutral
    @DeleteMapping
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<Void> unregister(
            @AuthenticationPrincipal(expression = "userId") String userId,
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestBody UnregisterDTO body) {
        if (body != null && body.token() != null) {
            repo.findByToken(body.token().trim())
                    .filter(d -> d.getUserId().equals(userId))
                    .ifPresent(d -> repo.deleteByToken(d.getToken()));
        }
        return new ApiResponse<>("success", 200, "Notifications turned off for this phone", null);
    }
}
