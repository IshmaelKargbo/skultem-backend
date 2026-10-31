package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record EditSubjectGroupDTO(
        @NotBlank(message = "Name is required") String name,
        @Min(value = 1, message = "Total selection must be at least 1") int totalSelection) {
}
