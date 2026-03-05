package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAssessmentTemplateDTO(
        @NotBlank(message = "Template name is required") String name,
        @NotBlank(message = "Template description is required") String description) {
}
