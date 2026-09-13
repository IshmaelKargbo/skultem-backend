package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AssignTimingLevelDTO(
        @Pattern(regexp = "PRIMARY|JSS|SSS", message = "Level is invalid")
        String level,

        @NotBlank(message = "Timing template is required")
        String timingId

) {}
