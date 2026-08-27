package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record ReopenAssessmentDTO(
        @NotBlank(message = "Assessment id is required!") String assessmentId,
        @NotBlank(message = "Term id is required!") String termId,
        @NotBlank(message = "A reason for reopening this assessment is required!") String note) {
}
