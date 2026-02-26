package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateStreamDTO(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Description is required") String description) {
}
