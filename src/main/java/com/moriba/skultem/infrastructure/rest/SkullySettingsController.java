package com.moriba.skultem.infrastructure.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.ModuleAccessService;
import com.moriba.skultem.application.services.SkullyReminderService;
import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.security.SectionNeutral;

import lombok.RequiredArgsConstructor;

/** A school's Skully page: which reminders Skully sends its parents, and how many went out this month. */
@RestController
@RequestMapping("/api/v1/skully")
@RequiredArgsConstructor
public class SkullySettingsController {

    private final SkullyReminderService reminders;
    private final ModuleAccessService modules;

    @SectionNeutral
    @GetMapping("/settings")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PRINCIPAL', 'SUPER_ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<SkullyReminderService.SettingsView> settings(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return new ApiResponse<>("success", 200, "Skully settings", reminders.view(school));
    }

    // Parents who haven't had the WhatsApp welcome (how to sign in) yet, and why.
    @SectionNeutral
    @GetMapping("/welcome")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PRINCIPAL', 'SUPER_ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<SkullyReminderService.WelcomeTracking> welcome(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return new ApiResponse<>("success", 200, "Skully welcome", reminders.welcomeTracking(school));
    }

    // Changing what goes to every parent is for whole-school leadership (not section-limited admins).
    @PutMapping("/settings")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PRINCIPAL', 'SUPER_ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<SkullyReminderService.SettingsView> save(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestBody SkullyReminderService.Settings body) {
        if (!modules.isEnabled(school, FeatureModule.WHATSAPP_ASSISTANT)) {
            throw new RuleException("Skully isn't switched on for your school yet. Contact Skultem to get it.");
        }
        return new ApiResponse<>("success", 200, "Skully settings saved", reminders.saveSettings(school, body));
    }
}
