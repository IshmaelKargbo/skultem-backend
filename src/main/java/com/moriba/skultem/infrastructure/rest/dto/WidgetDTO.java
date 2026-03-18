package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WidgetDTO(

    @NotBlank(message = "Entity must not be blank")
    String entity,

    @Valid
    List<FilterDTO> filters, // optional, can be empty

    @NotEmpty(message = "At least one metric is required")
    @Valid
    List<MetricDTO> metrics,

    @NotBlank(message = "Chart type must not be blank")
    @Pattern(
        regexp = "bar|line|pie|table",
        flags = Pattern.Flag.CASE_INSENSITIVE,
        message = "Chart type must be one of: bar, line, pie, table"
    )
    String chartType,

    @NotBlank(message = "Widget title must not be blank")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    String title

) {}