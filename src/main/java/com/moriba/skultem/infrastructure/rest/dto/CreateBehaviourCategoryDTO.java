package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBehaviourCategoryDTO(
        @NotBlank(message = "Name are required") String name,

        @NotBlank(message = "Description are required") String description) {
}
