package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MetricDTO(

        @NotBlank(message = "Metric name must not be blank") String name,

        @NotBlank(message = "Aggregation type must not be blank") @Pattern(regexp = "sum|avg|count|percentage|custom", flags = Pattern.Flag.CASE_INSENSITIVE, message = "Aggregation must be one of: sum, avg, count, percentage, custom") String aggregation,

        // Field is required for sum, avg, percentage; optional for count
        String field,

        Map<String, String> tags,

        List<MetricFilterDTO> filters,

        Double value) {
}