package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record UpdateGradingScaleDTO(
        @NotEmpty(message = "At least one grade band is required") @Valid List<GradeBandInput> bands) {

    public record GradeBandInput(
            @Min(value = 0, message = "minScore must be >= 0") @Max(value = 100, message = "minScore must be <= 100") int minScore,
            @Min(value = 0, message = "maxScore must be >= 0") @Max(value = 100, message = "maxScore must be <= 100") int maxScore,
            @NotBlank(message = "grade is required") String grade) {
    }
}
