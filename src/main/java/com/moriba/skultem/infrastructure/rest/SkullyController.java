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
import com.moriba.skultem.application.services.SkullyReminderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final com.moriba.skultem.application.services.SkullyStaffService staff;
    private final SkullyReminderService reminders;

    @Value("${skully.api-key:}")
    private String apiKey;

    @GetMapping("/family")
    public ResponseEntity<ApiResponse<SkullyParentService.Family>> family(
            @RequestHeader(value = "X-Skully-Key", required = false) String key,
            @RequestParam String phone) {
        var denied = check(key);
        if (denied != null) return denied.build();
        return ResponseEntity.ok(new ApiResponse<>("success", 200, "Family found",
                skully.family(phone).withStaff(staff.staff(phone))));
    }

    // A teacher's own classes: the class they are class teacher of and the subjects they teach (SkullyStaffService).
    @GetMapping("/staff/classes")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> staffClasses(
            @RequestHeader(value = "X-Skully-Key", required = false) String key,
            @RequestParam String phone, @RequestParam String schoolId) {
        var denied = check(key);
        if (denied != null) return denied.build();
        return staff.myClasses(phone, schoolId)
                .map(o -> ResponseEntity.ok(new ApiResponse<>("success", 200, "My classes", o)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // A staff member's school, limited to what their role sees in Skultem (see SkullyStaffService).
    @GetMapping("/staff/overview")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> staffOverview(
            @RequestHeader(value = "X-Skully-Key", required = false) String key,
            @RequestParam String phone, @RequestParam String schoolId) {
        var denied = check(key);
        if (denied != null) return denied.build();
        return staff.overview(phone, schoolId)
                .map(o -> ResponseEntity.ok(new ApiResponse<>("success", 200, "School overview", o)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
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

    // A parent's portal sign-in details (a pending account's temporary password included) - see SkullyParentService.
    @GetMapping("/portal-access")
    public ResponseEntity<ApiResponse<List<SkullyParentService.PortalAccess>>> portalAccess(
            @RequestHeader(value = "X-Skully-Key", required = false) String key,
            @RequestParam String phone) {
        var denied = check(key);
        if (denied != null) return denied.build();
        return ResponseEntity.ok(new ApiResponse<>("success", 200, "Portal access", skully.portalAccess(phone)));
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

    // Messages Skully should send now: welcomes, linked children and reminders (each parent at most one reminder
    // message a day per school). kinds = the ones Skully has a WhatsApp template for; an older Skully that doesn't
    // say gets reminders only. They count as handed over until Skully reports back with /reminders/result.
    @GetMapping("/reminders/due")
    public ResponseEntity<ApiResponse<List<SkullyReminderService.Message>>> dueReminders(
            @RequestHeader(value = "X-Skully-Key", required = false) String key,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = SkullyReminderService.REMINDER) List<String> kinds) {
        var denied = check(key);
        if (denied != null) return denied.build();
        var wanted = new java.util.HashSet<>(kinds.stream().map(k -> k.trim().toUpperCase()).toList());
        wanted.retainAll(SkullyReminderService.ALL_KINDS);
        var res = reminders.due(Math.max(1, Math.min(limit, 500)), wanted);
        return ResponseEntity.ok(new ApiResponse<>("success", 200, "Reminders due", res));
    }

    public record ReminderResult(List<String> sent, List<String> failed) {
    }

    @PostMapping("/reminders/result")
    public ResponseEntity<ApiResponse<Void>> reminderResult(
            @RequestHeader(value = "X-Skully-Key", required = false) String key,
            @RequestBody ReminderResult body) {
        var denied = check(key);
        if (denied != null) return denied.build();
        reminders.result(body.sent(), body.failed());
        return ResponseEntity.ok(new ApiResponse<>("success", 200, "Recorded", null));
    }

    public record OptOut(String phone, boolean optOut) {
    }

    // A parent replied STOP (optOut = true) or START to Skully.
    @PostMapping("/reminders/opt-out")
    public ResponseEntity<ApiResponse<Void>> optOut(
            @RequestHeader(value = "X-Skully-Key", required = false) String key,
            @RequestBody OptOut body) {
        var denied = check(key);
        if (denied != null) return denied.build();
        reminders.optOut(body.phone(), body.optOut());
        return ResponseEntity.ok(new ApiResponse<>("success", 200, "Recorded", null));
    }

    private ResponseEntity.BodyBuilder check(String key) {
        if (apiKey == null || apiKey.isBlank()) return ResponseEntity.status(HttpStatus.NOT_FOUND);
        boolean ok = key != null && MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8));
        return ok ? null : ResponseEntity.status(HttpStatus.UNAUTHORIZED);
    }
}
