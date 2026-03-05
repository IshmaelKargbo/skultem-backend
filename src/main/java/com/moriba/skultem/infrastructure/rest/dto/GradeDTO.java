package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GradeDTO(
                @NotBlank(message = "Assessment ID is required") String id,
                @NotNull(message = "Score is required") int score) {
}
