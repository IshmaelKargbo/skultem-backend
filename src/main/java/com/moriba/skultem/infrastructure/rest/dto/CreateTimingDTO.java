package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateTimingDTO(
        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        @Min(value = 1, message = "Period duration must be greater than 0")
        int periodDuration,

        @Min(value = 0, message = "Break duration cannot be negative")
        int breakDuration,

        @Min(value = 0, message = "Lunch duration cannot be negative")
        int lunchDuration

) {}