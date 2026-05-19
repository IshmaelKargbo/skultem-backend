package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateAssessmentTemplateDTO(

        @NotBlank(message = "Template name is required")
        String name,
        @Min(value = 0, message = "Pass mark cannot be less than 0")
        @Max(value = 100, message = "Pass mark cannot be greater than 100")
        int passMark,
        @NotBlank(message = "Template description is required")
        String description

) {

}
