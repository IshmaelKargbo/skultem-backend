package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveAttendanceLocationSettingDTO(

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
        Double longitude,

        @NotNull(message = "Radius is required")
        @Min(value = 10, message = "Radius must be at least 10 metres")
        Integer radiusMeters,

        @Size(max = 500, message = "Allowed IPs must not exceed 500 characters")
        String allowedIps

) {
}
