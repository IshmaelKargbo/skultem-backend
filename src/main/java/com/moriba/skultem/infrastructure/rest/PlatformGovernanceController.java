package com.moriba.skultem.infrastructure.rest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.AuditEntryDTO;
import com.moriba.skultem.application.services.AuditTrailService;
import com.moriba.skultem.application.services.PlatformAccessService;
import com.moriba.skultem.application.services.PlatformSettingsService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.security.PermissionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

// Who can do what on the platform, what has happened on it, and the platform's own settings. Each part asks for its
// own permission, so an auditor can read the audit log without being able to change a setting.
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class PlatformGovernanceController {

    private final PlatformAccessService access;
    private final PlatformSettingsService settings;
    private final AuditTrailService audit;
    private final PermissionService permissions;
    private final com.moriba.skultem.application.services.PlatformFeeService platformFee;

    private static Map<String, Object> meta(Page<?> res) {
        return Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());
    }

    // ---- What the signed-in system admin may do (the app uses it to show only what they can open) ----

    public record Me(String role, String roleLabel, List<String> permissions) {
    }

    @GetMapping("/me/access")
    @PreAuthorize("@permissionService.isSystemAdmin()")
    public ApiResponse<Me> me() {
        var role = access.roleOf(permissions.currentUserId());
        return new ApiResponse<>("success", 200, "Access fetched successfully",
                new Me(role.name(), role.label, role.permissions.stream().map(Enum::name).sorted().toList()));
    }

    // ---- Roles & permissions ----

    public record RoleInfo(String role, String label, String description, List<String> permissions) {
    }

    public record StaffInfo(String userId, String name, String email, String status, String role, String roleLabel,
            boolean explicit, Instant lastLoginAt) {
        static StaffInfo of(PlatformAccessService.Staff s) {
            return new StaffInfo(s.userId(), s.name(), s.email(), s.status(), s.role().name(), s.role().label,
                    s.explicit(), s.lastLoginAt());
        }
    }

    public record RolesView(List<RoleInfo> roles, List<StaffInfo> staff) {
    }

    @GetMapping("/roles")
    @PreAuthorize("@permissionService.hasPlatformPermission('ROLES')")
    public ApiResponse<RolesView> roles() {
        var roles = java.util.Arrays.stream(PlatformAccessService.Role.values())
                .map(r -> new RoleInfo(r.name(), r.label, r.description, r.permissions.stream().map(Enum::name).sorted().toList()))
                .toList();
        var staff = access.staff().stream().map(StaffInfo::of).toList();
        return new ApiResponse<>("success", 200, "Roles fetched successfully", new RolesView(roles, staff));
    }

    public record AssignRole(@NotBlank(message = "Choose a role") String role) {
    }

    @PutMapping("/roles/{userId}")
    @PreAuthorize("@permissionService.hasPlatformPermission('ROLES')")
    public ApiResponse<StaffInfo> assign(@AuthenticationPrincipal(expression = "userId") String actorId,
            @PathVariable String userId, @Valid @RequestBody AssignRole param) {
        return new ApiResponse<>("success", 200, "Role updated successfully",
                StaffInfo.of(access.assign(userId, param.role(), actorId)));
    }

    // ---- The platform fee: one flat amount per student per term, for every school ----

    @GetMapping("/platform-fee")
    @PreAuthorize("@permissionService.hasPlatformPermission('SETTINGS')")
    public ApiResponse<com.moriba.skultem.application.services.PlatformFeeService.Overview> platformFee() {
        return new ApiResponse<>("success", 200, "Platform fee fetched successfully", platformFee.overview());
    }

    public record SetPlatformFee(@NotNull(message = "Enter the fee per term") java.math.BigDecimal perTerm) {
    }

    @PutMapping("/platform-fee")
    @PreAuthorize("@permissionService.hasPlatformPermission('SETTINGS')")
    public ApiResponse<com.moriba.skultem.application.services.PlatformFeeService.Overview> setPlatformFee(
            @Valid @RequestBody SetPlatformFee param) {
        return new ApiResponse<>("success", 200, "Platform fee saved successfully", platformFee.setForAllSchools(param.perTerm()));
    }

    @GetMapping("/platform-fee/collections")
    @PreAuthorize("@permissionService.hasPlatformPermission('BILLING')")
    public ApiResponse<com.moriba.skultem.application.services.PlatformFeeService.Collections> platformFeeCollections() {
        return new ApiResponse<>("success", 200, "Platform fee collections fetched successfully", platformFee.collections());
    }

    // ---- Settings ----

    public record SettingInfo(String key, String group, String label, String description, String type, String value,
            String defaultValue, Integer min, Integer max, boolean changed, String updatedByName, Instant updatedAt) {
    }

    private static List<SettingInfo> view(List<PlatformSettingsService.Entry> entries) {
        return entries.stream().map(e -> new SettingInfo(e.definition().key(), e.definition().group(),
                e.definition().label(), e.definition().description(), e.definition().type().name(), e.value(),
                e.definition().defaultValue(), e.definition().min(), e.definition().max(), e.changed(),
                e.updatedByName(), e.updatedAt())).toList();
    }

    @GetMapping("/settings")
    @PreAuthorize("@permissionService.hasPlatformPermission('SETTINGS')")
    public ApiResponse<List<SettingInfo>> settings() {
        return new ApiResponse<>("success", 200, "Settings fetched successfully", view(settings.all()));
    }

    public record SaveSettings(@NotNull(message = "Nothing to save") Map<String, String> values) {
    }

    @PutMapping("/settings")
    @PreAuthorize("@permissionService.hasPlatformPermission('SETTINGS')")
    public ApiResponse<List<SettingInfo>> saveSettings(@AuthenticationPrincipal(expression = "userId") String actorId,
            @Valid @RequestBody SaveSettings param) {
        return new ApiResponse<>("success", 200, "Settings saved successfully", view(settings.save(param.values(), actorId)));
    }

    // ---- Audit log (read-only: there is no way to change or remove an entry through the API) ----

    @GetMapping("/audit-logs")
    @PreAuthorize("@permissionService.hasPlatformPermission('AUDIT')")
    public ApiResponse<List<AuditEntryDTO>> auditLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "20") Integer size, @RequestParam(defaultValue = "1") Integer page) {
        var res = audit.list(new AuditTrailService.Filter(search, status, schoolId, from, to), page, Math.min(size, 100));
        return new ApiResponse<>("success", 200, "Audit log fetched successfully", res.getContent(), meta(res));
    }
}
