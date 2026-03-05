package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitAssessmentDTO(
        @NotBlank(message = "Assessment id is required!") String assessmentId,
        @NotBlank(message = "Term id is required!") String termId,
        @NotBlank(message = "Submission note is required!") String note) {
}
