package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWeekDTO(

        @NotBlank(message = "Scheme is required")
        String scheme,

        @NotBlank(message = "Topic is required")
        String topic,

        String subtopic,

        @NotNull(message = "Week is required")
        @Min(value = 1, message = "Week must be greater than 0")
        Integer week,

        @NotEmpty(message = "At least one objective is required")
        @Size(min = 1, message = "At least one objective is required")
        List<String> objectives

) {}