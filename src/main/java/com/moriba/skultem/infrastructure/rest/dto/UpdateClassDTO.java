package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateClassDTO(
        @NotBlank(message = "Name is required") String name,

        // Optional - left out, the class keeps its current position (see UpdateClassUseCase).
        Integer levelOrder) {
}
