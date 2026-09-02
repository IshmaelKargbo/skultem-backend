package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.Getter;

// The school's GPS location + how far a teacher can be from it and still be allowed to clock in.
// One row per school, same "single active config" pattern as ReceiptSetting. allowedIps is a
// second, optional layer: GPS alone only stops someone who can't fake their browser's location,
// which is trivial for anyone even slightly technical. Pinning clock-in to the school's own
// network makes "clock in for a friend" require being physically on that network too - but it's
// left optional because plenty of schools don't have a stable public IP to pin to.
@Getter
public class AttendanceLocationSetting extends AggregateRoot<String> {

    private String schoolId;
    private double latitude;
    private double longitude;
    private int radiusMeters;
    private String allowedIps;

    public AttendanceLocationSetting(String id, String schoolId, double latitude, double longitude,
            int radiusMeters, String allowedIps, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        this.allowedIps = allowedIps;
        touch(updatedAt);
    }

    public static AttendanceLocationSetting create(String id, String schoolId, double latitude, double longitude,
            int radiusMeters, String allowedIps) {
        Instant now = Instant.now();
        return new AttendanceLocationSetting(id, schoolId, latitude, longitude, radiusMeters, allowedIps, now, now);
    }

    public void update(double latitude, double longitude, int radiusMeters, String allowedIps) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        this.allowedIps = allowedIps;
        touch(Instant.now());
    }

    public boolean hasIpRestriction() {
        return allowedIps != null && !allowedIps.isBlank();
    }

    // allowedIps is a comma-separated list of exact IPs and/or IPv4 CIDR ranges
    // (e.g. "41.66.12.5, 192.168.1.0/24"). No restriction at all if left blank.
    public boolean isIpAllowed(String ip) {
        if (!hasIpRestriction() || ip == null || ip.isBlank()) {
            return !hasIpRestriction();
        }

        for (String entry : allowedIps.split(",")) {
            String candidate = entry.trim();
            if (candidate.isEmpty()) continue;

            if (candidate.contains("/")) {
                if (isInCidrRange(ip, candidate)) return true;
            } else if (candidate.equals(ip)) {
                return true;
            }
        }

        return false;
    }

    private boolean isInCidrRange(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            int prefixLength = Integer.parseInt(parts[1]);
            long mask = prefixLength == 0 ? 0 : (0xFFFFFFFFL << (32 - prefixLength)) & 0xFFFFFFFFL;

            return (ipToLong(ip) & mask) == (ipToLong(parts[0]) & mask);
        } catch (Exception e) {
            // Malformed CIDR entry (or a non-IPv4 address) - fail closed for that entry rather
            // than let a typo in settings silently open access.
            return false;
        }
    }

    private long ipToLong(String ip) {
        String[] octets = ip.trim().split("\\.");
        if (octets.length != 4) throw new IllegalArgumentException("Not an IPv4 address: " + ip);

        long result = 0;
        for (String octet : octets) {
            result = (result << 8) | (Integer.parseInt(octet) & 0xFF);
        }
        return result;
    }

    // Haversine formula - great-circle distance between this point and (lat, lng), in metres.
    // Accurate enough for a school-sized radius; no need for anything more precise here.
    public double distanceMetersTo(double lat, double lng) {
        double earthRadiusMeters = 6371000;

        double dLat = Math.toRadians(lat - latitude);
        double dLng = Math.toRadians(lng - longitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(lat))
                        * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusMeters * c;
    }

    public boolean isWithinRange(double lat, double lng) {
        return distanceMetersTo(lat, lng) <= radiusMeters;
    }
}
