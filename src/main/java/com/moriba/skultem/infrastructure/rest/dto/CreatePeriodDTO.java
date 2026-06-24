package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePeriodDTO(
        @NotBlank(message = "Session is required")
        String session
) {

}