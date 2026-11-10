package com.moriba.skultem.infrastructure.rest;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.domain.model.PushSubscription;
import com.moriba.skultem.domain.repository.PushSubscriptionRepository;
import com.moriba.skultem.infrastructure.push.WebPushService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.security.SectionNeutral;

import lombok.RequiredArgsConstructor;

// Native notifications on a device: the app asks for the server's public key, subscribes the device with the
// browser's push service, and registers that subscription here. Every school role can use it, for themselves.
@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final WebPushService pushService;
    private final PushSubscriptionRepository repo;

    public record SubscriptionKeys(String p256dh, String auth) {
    }

    public record SubscribeDTO(String endpoint, SubscriptionKeys keys) {
    }

    public record UnsubscribeDTO(String endpoint) {
    }

    @SectionNeutral
    @GetMapping("/public-key")
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<Map<String, Object>> publicKey(@AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return new ApiResponse<>("success", 200, "Push key",
                Map.of("enabled", pushService.isEnabled(), "publicKey", pushService.publicKey()));
    }

    @SectionNeutral
    @PostMapping("/subscriptions")
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<Void> subscribe(
            @AuthenticationPrincipal(expression = "userId") String userId,
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestBody SubscribeDTO body) {
        if (body == null || body.endpoint() == null || !body.endpoint().startsWith("https://") || body.keys() == null
                || body.keys().p256dh() == null || body.keys().auth() == null) {
            throw new BadRequestException("That isn't a valid push subscription");
        }
        String agent = userAgent == null ? null : userAgent.substring(0, Math.min(512, userAgent.length()));
        var existing = repo.findByEndpoint(body.endpoint()).orElse(null);
        if (existing != null && !existing.getUserId().equals(userId)) {
            // The same device now belongs to someone else signed in on it - it follows the new user.
            repo.deleteByEndpoint(body.endpoint());
            existing = null;
        }
        if (existing == null) {
            repo.save(PushSubscription.create(userId, school, body.endpoint(), body.keys().p256dh(), body.keys().auth(), agent));
        } else {
            existing.refresh(school, body.keys().p256dh(), body.keys().auth(), agent);
            repo.save(existing);
        }
        return new ApiResponse<>("success", 200, "Notifications turned on for this device", null);
    }

    @SectionNeutral
    @DeleteMapping("/subscriptions")
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<Void> unsubscribe(
            @AuthenticationPrincipal(expression = "userId") String userId,
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestBody UnsubscribeDTO body) {
        if (body != null && body.endpoint() != null) {
            repo.findByEndpoint(body.endpoint())
                    .filter(s -> s.getUserId().equals(userId))
                    .ifPresent(s -> repo.deleteByEndpoint(s.getEndpoint()));
        }
        return new ApiResponse<>("success", 200, "Notifications turned off for this device", null);
    }

    // Sends a test notification to all of your devices.
    @SectionNeutral
    @PostMapping("/test")
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<Map<String, Integer>> test(
            @AuthenticationPrincipal(expression = "userId") String userId,
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        int sent = pushService.sendToUser(userId, Map.of(
                "title", "Notifications are on",
                "body", "You'll get Skultem notifications on this device.",
                "tag", "skultem-test",
                "url", "/profile"));
        return new ApiResponse<>("success", 200, "Test sent", Map.of("devices", sent));
    }
}
