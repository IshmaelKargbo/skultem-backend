package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssessmentAssignmentDTO(
        @NotBlank(message = "Assessment session is required") String name,
        @NotNull(message = "Assessment weight is required") Integer weight,
        // Term numbers the test runs in; empty or missing = every term.
        java.util.List<Integer> terms) {
}
