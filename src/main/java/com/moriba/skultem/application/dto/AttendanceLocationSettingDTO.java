package com.moriba.skultem.application.dto;

public record AttendanceLocationSettingDTO(
        boolean configured,
        double latitude,
        double longitude,
        int radiusMeters,
        String allowedIps) {
}
