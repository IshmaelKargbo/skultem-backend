package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBehaviourDTO(
        @NotBlank(message = "Category are required") String category,

        @NotBlank(message = "Enrollment are required") String enrollment,

        @NotBlank(message = "Note is required") @Size(min = 2, max = 250, message = "Note must be between 2 and 100 characters") String note,

        @NotBlank(message = "Kind is required") @Pattern(regexp = "POSITIVE|NEGATIVE|NEUTRAL", message = "Kind must be POSITIVE, NEGATIVE or NEUTRAL") String kind) {
}
