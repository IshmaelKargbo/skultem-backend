package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveBlogCategoryDTO(
        @NotBlank(message = "Name is required") @Size(max = 80, message = "Name must be at most 80 characters") String name,
        // Optional: made from the name when left blank.
        @Size(max = 120, message = "Slug must be at most 120 characters") @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug may only have lowercase letters, numbers and single hyphens between them") String slug,
        @Size(max = 500, message = "Description must be at most 500 characters") String description,
        Integer displayOrder) {
}
