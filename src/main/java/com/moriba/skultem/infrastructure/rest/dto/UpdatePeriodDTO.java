package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record UpdatePeriodDTO(
        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime
) {
}
