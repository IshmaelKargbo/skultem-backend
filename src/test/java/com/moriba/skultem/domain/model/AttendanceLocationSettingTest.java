package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;

import org.junit.jupiter.api.Test;

// Plain unit tests - no Spring context needed, this is pure domain logic. Covers the geofence
// distance/range math and the IP allowlist/CIDR matching used by ClockInUseCase/ClockOutUseCase
// to decide whether a teacher is allowed to self-service clock in/out.
class AttendanceLocationSettingTest {

    private AttendanceLocationSetting settingAt(double lat, double lng, int radiusMeters, String allowedIps) {
        Instant now = Instant.now();
        return new AttendanceLocationSetting("ALS-1", "school-1", lat, lng, radiusMeters, allowedIps, now, now);
    }

    @Test
    void distanceMetersToIsZeroAtTheSamePoint() {
        var setting = settingAt(8.4672512, -13.2317184, 150, null);

        assertThat(setting.distanceMetersTo(8.4672512, -13.2317184)).isCloseTo(0, within(0.01));
    }

    @Test
    void distanceMetersToMatchesTheKnownOneDegreeOfLongitudeAtTheEquator() {
        // A textbook reference distance: one degree of longitude at the equator is ~111.32km.
        var setting = settingAt(0, 0, 150, null);

        assertThat(setting.distanceMetersTo(0, 1)).isCloseTo(111_320, within(200.0));
    }

    @Test
    void isWithinRangeIsTrueJustInsideTheRadiusAndFalseJustOutsideIt() {
        // ~111.32m north of the school (0.001 degrees of latitude at any longitude).
        var setting = settingAt(0, 0, 150, null);

        assertThat(setting.isWithinRange(0.001, 0)).isTrue();

        var tighter = settingAt(0, 0, 100, null);
        assertThat(tighter.isWithinRange(0.001, 0)).isFalse();
    }

    @Test
    void hasIpRestrictionIsFalseWhenAllowedIpsIsNullOrBlank() {
        assertThat(settingAt(0, 0, 150, null).hasIpRestriction()).isFalse();
        assertThat(settingAt(0, 0, 150, "").hasIpRestriction()).isFalse();
        assertThat(settingAt(0, 0, 150, "   ").hasIpRestriction()).isFalse();
        assertThat(settingAt(0, 0, 150, "41.66.12.5").hasIpRestriction()).isTrue();
    }

    @Test
    void isIpAllowedAllowsAnyIpWhenNoRestrictionIsConfigured() {
        var setting = settingAt(0, 0, 150, null);

        assertThat(setting.isIpAllowed("41.66.12.5")).isTrue();
        assertThat(setting.isIpAllowed(null)).isTrue();
    }

    @Test
    void isIpAllowedMatchesAnExactIpInTheList() {
        var setting = settingAt(0, 0, 150, "41.66.12.5, 102.4.5.6");

        assertThat(setting.isIpAllowed("41.66.12.5")).isTrue();
        assertThat(setting.isIpAllowed("102.4.5.6")).isTrue();
        assertThat(setting.isIpAllowed("41.66.12.6")).isFalse();
    }

    @Test
    void isIpAllowedMatchesAnAddressInsideAnAllowedCidrRange() {
        var setting = settingAt(0, 0, 150, "192.168.1.0/24");

        assertThat(setting.isIpAllowed("192.168.1.1")).isTrue();
        assertThat(setting.isIpAllowed("192.168.1.254")).isTrue();
        assertThat(setting.isIpAllowed("192.168.2.1")).isFalse();
    }

    @Test
    void isIpAllowedDeniesWhenRestrictedAndNoIpWasProvided() {
        var setting = settingAt(0, 0, 150, "192.168.1.0/24");

        assertThat(setting.isIpAllowed(null)).isFalse();
        assertThat(setting.isIpAllowed("")).isFalse();
    }

    @Test
    void isIpAllowedFailsClosedOnAMalformedCidrEntryInsteadOfThrowing() {
        // A typo in settings (or a non-IPv4 address, e.g. IPv6) must never silently open access.
        var setting = settingAt(0, 0, 150, "not-a-cidr/24");

        assertThat(setting.isIpAllowed("192.168.1.1")).isFalse();
    }

    @Test
    void isIpAllowedFailsClosedOnAnIpv6ClientAddress() {
        var setting = settingAt(0, 0, 150, "192.168.1.0/24");

        assertThat(setting.isIpAllowed("::1")).isFalse();
    }
}
