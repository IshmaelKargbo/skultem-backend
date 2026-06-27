package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSchemeOfWorkDTO(
        @NotBlank(message = "Subject is required")
        String subject,

        @NotBlank(message = "Class is required")
        String session,

        @NotBlank(message = "Term is required")
        String term
) {}