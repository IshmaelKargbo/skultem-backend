package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record AssessmentActionDTO(
        @NotBlank(message = "Note is required!") String note) {
}
