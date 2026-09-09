package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateClassDTO(
        @NotBlank(message = "Name is required") String name,

        @NotNull(message = "Level order is required") Integer levelOrder) {
}
