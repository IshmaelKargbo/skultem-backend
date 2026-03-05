package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssessmentAssignmentDTO(
        @NotBlank(message = "Assessment name is required") String name,
        @NotNull(message = "Assessment weight is required") Integer weight) {
}
