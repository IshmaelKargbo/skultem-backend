package com.moriba.skultem.application.dto;

// managementSectionId null = the school-wide location; otherwise that section's own.
public record AttendanceLocationSettingDTO(
        boolean configured,
        double latitude,
        double longitude,
        int radiusMeters,
        String allowedIps,
        String managementSectionId) {
}
