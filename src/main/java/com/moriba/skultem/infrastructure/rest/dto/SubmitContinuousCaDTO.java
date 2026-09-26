package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

public record SubmitContinuousCaDTO(
        @NotNull(message = "Assessment id is required") String assessmentId,
        @NotNull(message = "Term id is required") String termId) {
}
