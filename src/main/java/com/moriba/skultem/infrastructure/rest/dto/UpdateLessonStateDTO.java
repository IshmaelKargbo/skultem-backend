package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateLessonStateDTO(

        @NotBlank(message = "State is required")
        @Pattern(regexp = "NOT_STARTED|IN_PROGRESS|COMPLETED", message = "State must be one of NOT_STARTED, IN_PROGRESS, COMPLETED")
        String state

) {}
