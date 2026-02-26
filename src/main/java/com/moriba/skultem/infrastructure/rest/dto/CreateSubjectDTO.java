package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSubjectDTO(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Code is required") String code,
        @NotBlank(message = "Description is required") String description) {
}
