package com.moriba.skultem.infrastructure.rest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.services.SkullyParentService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * The API Skully (the parents' WhatsApp assistant, a Cloudflare Worker in /skully) reads from. It is not a user
 * session: the Worker sends the shared key in X-Skully-Key, and every call names the parent's WhatsApp number, which
 * is all a parent can see. Off (404) until SKULLY_API_KEY is set.
 */
@RestController
@RequestMapping("/api/v1/integrations/skully")
@RequiredArgsConstructor
public class SkullyController {

    private final SkullyParentService skully;

    @Value("${skully.api-key:}")
    private String apiKey;

    @GetMapping("/family")
    public ResponseEntity<ApiResponse<SkullyParentService.Family>> family(
            @RequestHeader(value = "X-Skully-Key", required = false) String key,
            @RequestParam String phone) {
        var denied = check(key);
        if (denied != null) return denied.build();
        return ResponseEntity.ok(new ApiResponse<>("success", 200, "Family found", skully.family(phone)));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<SkullyParentService.ChildSummary>> child(
            @RequestHeader(value = "X-Skully-Key", required = false) String key,
            @PathVariable String studentId,
            @RequestParam String phone) {
        var denied = check(key);
        if (denied != null) return denied.build();
        return skully.child(phone, studentId)
                .map(c -> ResponseEntity.ok(new ApiResponse<>("success", 200, "Child found", c)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/school/{schoolId}/updates")
    public ResponseEntity<ApiResponse<List<SkullyParentService.Update>>> updates(
            @RequestHeader(value = "X-Skully-Key", required = false) String key,
            @PathVariable String schoolId,
            @RequestParam String phone) {
        var denied = check(key);
        if (denied != null) return denied.build();
        return skully.updates(phone, schoolId)
                .map(u -> ResponseEntity.ok(new ApiResponse<>("success", 200, "Updates found", u)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    private ResponseEntity.BodyBuilder check(String key) {
        if (apiKey == null || apiKey.isBlank()) return ResponseEntity.status(HttpStatus.NOT_FOUND);
        boolean ok = key != null && MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8));
        return ok ? null : ResponseEntity.status(HttpStatus.UNAUTHORIZED);
    }
}
