package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ClockInDTO(

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
        Double longitude,

        // The device's reported GPS accuracy radius in metres (position.coords.accuracy).
        // Optional - older clients that don't send it just get no tolerance widening.
        @DecimalMin(value = "0", message = "Accuracy cannot be negative")
        Double accuracy

) {
}
