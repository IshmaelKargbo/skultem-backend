package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Locking or unlocking one CA recording slot. `reason` is required when unlocking.
public record ContinuousWeekDTO(
        @NotNull(message = "Assessment id is required") String assessmentId,
        @NotNull(message = "Term id is required") String termId,
        @Min(value = 1, message = "Recording number starts at 1") int week,
        String reason) {
}
