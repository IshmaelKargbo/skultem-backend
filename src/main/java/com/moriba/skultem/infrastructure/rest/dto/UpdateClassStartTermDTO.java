package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateClassStartTermDTO(
        @NotNull(message = "Term is required") @Min(value = 1, message = "Term must be 1 or more") Integer termNumber) {
}
