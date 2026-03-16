package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveReportDTO(
        String id,
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Entity is required") String entity,
        @NotNull(message = "Filters cannot be null") List<FilterDTO> filters) {
}
