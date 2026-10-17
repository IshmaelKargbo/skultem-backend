package com.moriba.skultem.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.PlatformAccessService.Permission;
import com.moriba.skultem.application.services.PlatformAccessService.Role;

class PlatformGovernanceTest {

    @Test
    void anOwnerCanDoEverything() {
        assertEquals(EnumSet.allOf(Permission.class), Role.OWNER.permissions);
    }

    @Test
    void everyRoleCanSeeTheDashboard() {
        for (Role role : Role.values()) {
            assertTrue(role.permissions.contains(Permission.DASHBOARD), role + " should see the dashboard");
        }
    }

    @Test
    void onlyAnOwnerCanManageRolesOrSettings() {
        for (Role role : Role.values()) {
            if (role == Role.OWNER) {
                continue;
            }
            assertFalse(role.permissions.contains(Permission.ROLES), role + " must not manage roles");
            assertFalse(role.permissions.contains(Permission.SETTINGS), role + " must not change settings");
            assertFalse(role.permissions.contains(Permission.ADMINS), role + " must not manage admins");
        }
    }

    @Test
    void aBillingManagerSeesBillingButNotMarketing() {
        assertTrue(Role.BILLING.permissions.contains(Permission.BILLING));
        assertFalse(Role.BILLING.permissions.contains(Permission.MARKETING));
        assertFalse(Role.BILLING.permissions.contains(Permission.AUDIT));
    }

    @Test
    void anAuditorCanOnlyReadTheAuditLog() {
        assertEquals(EnumSet.of(Permission.DASHBOARD, Permission.AUDIT, Permission.HEALTH), Role.AUDITOR.permissions);
    }

    private static PlatformSettingsService.Definition def(String key) {
        return PlatformSettingsService.definition(key);
    }

    @Test
    void anIntegerSettingMustBeAWholeNumberInRange() {
        var d = def(PlatformSettingsService.PAYMENT_TERMS_DAYS);
        assertEquals("21", PlatformSettingsService.validate(d, " 21 "));
        assertThrows(RuleException.class, () -> PlatformSettingsService.validate(d, "abc"));
        assertThrows(RuleException.class, () -> PlatformSettingsService.validate(d, "-1"));
        assertThrows(RuleException.class, () -> PlatformSettingsService.validate(d, "91"));
        assertThrows(RuleException.class, () -> PlatformSettingsService.validate(d, "1.5"));
    }

    @Test
    void anEmailSettingMayBeEmptyButNotMalformed() {
        var d = def(PlatformSettingsService.SUPPORT_EMAIL);
        assertEquals("", PlatformSettingsService.validate(d, "  "));
        assertEquals("help@skultem.com", PlatformSettingsService.validate(d, "Help@Skultem.com"));
        assertThrows(RuleException.class, () -> PlatformSettingsService.validate(d, "not-an-email"));
    }

    @Test
    void aPhoneSettingAllowsDigitsSpacesAndALeadingPlus() {
        var d = def(PlatformSettingsService.SUPPORT_PHONE);
        assertEquals("+232 76 000000", PlatformSettingsService.validate(d, "+232 76 000000"));
        assertThrows(RuleException.class, () -> PlatformSettingsService.validate(d, "call me"));
    }

    @Test
    void theNameCannotBeEmpty() {
        var d = def(PlatformSettingsService.PLATFORM_NAME);
        assertThrows(RuleException.class, () -> PlatformSettingsService.validate(d, " "));
        assertEquals("Skultem", PlatformSettingsService.validate(d, "Skultem"));
    }

    @Test
    void anUnknownSettingIsRefused() {
        assertThrows(RuleException.class, () -> PlatformSettingsService.definition("made.up.key"));
    }

    @Test
    void onlyTheMarketingSettingsArePublic() {
        var publicKeys = PlatformSettingsService.DEFINITIONS.stream()
                .filter(PlatformSettingsService.Definition::isPublic).map(PlatformSettingsService.Definition::key).toList();
        assertEquals(java.util.List.of("platform.name", "support.email", "support.phone"), publicKeys);
    }
}
