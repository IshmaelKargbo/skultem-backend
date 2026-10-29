package com.moriba.skultem.infrastructure.rest.dto;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;

public record SaveLibraryCategoryDTO(
        @NotBlank(message = "Name is required") @Length(max = 100, message = "Name must not exceed 100 characters") String name,
        @Length(max = 500, message = "Description must not exceed 500 characters") String description) {
}
