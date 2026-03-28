package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExpenseCategoryDTO(
        @NotBlank(message = "Name is required") String name,
        @NotNull(message = "Description is required") String description) {
}
