package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateClassTemplateDTO(
        @NotBlank(message = "Assessment template is required") String templateId) {
}
