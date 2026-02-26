package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record NextClassDTO(
        @NotBlank(message = "Id is required") String id,

        @NotBlank(message = "Next class is required") String nextClass) {
}
