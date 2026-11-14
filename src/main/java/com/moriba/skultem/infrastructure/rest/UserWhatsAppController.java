package com.moriba.skultem.infrastructure.rest;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.SkullyStaffService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.security.SectionNeutral;

import lombok.RequiredArgsConstructor;

/**
 * A user's WhatsApp number, so Skully recognises school staff (see SkullyStaffService). Anyone can set their own;
 * school leadership can set it for staff of their school.
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserWhatsAppController {

    private final SkullyStaffService staff;
    private final JdbcTemplate jdbc;

    public record WhatsApp(String phone) {
    }

    @SectionNeutral
    @GetMapping("/me/whatsapp")
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<WhatsApp> mine(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId) {
        return new ApiResponse<>("success", 200, "WhatsApp number", new WhatsApp(staff.phone(userId)));
    }

    @SectionNeutral
    @PutMapping("/me/whatsapp")
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<WhatsApp> saveMine(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId, @RequestBody WhatsApp body) {
        return new ApiResponse<>("success", 200, "WhatsApp number saved", new WhatsApp(staff.savePhone(userId, body.phone())));
    }

    @SectionNeutral
    @GetMapping("/{id}/whatsapp")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'PRINCIPAL', 'SUPER_ADMIN')")
    public ApiResponse<WhatsApp> theirs(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        requireMember(school, id);
        return new ApiResponse<>("success", 200, "WhatsApp number", new WhatsApp(staff.phone(id)));
    }

    @SectionNeutral
    @PutMapping("/{id}/whatsapp")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PRINCIPAL', 'SUPER_ADMIN', 'PROPRIETOR') and @permissionService.canManageUser(#school, #id)")
    public ApiResponse<WhatsApp> saveTheirs(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id, @RequestBody WhatsApp body) {
        requireMember(school, id);
        return new ApiResponse<>("success", 200, "WhatsApp number saved", new WhatsApp(staff.savePhone(id, body.phone())));
    }

    // Only someone who belongs to this school - never an account from another school.
    private void requireMember(String school, String userId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM school_users WHERE school_id = ? AND user_id = ?",
                Integer.class, school, userId);
        if (n == null || n == 0) throw new NotFoundException("User not found");
    }
}
