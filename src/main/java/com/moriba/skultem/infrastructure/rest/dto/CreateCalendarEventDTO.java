package com.moriba.skultem.infrastructure.rest.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCalendarEventDTO(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Description is required") String description,
        @NotBlank(message = "Type is required") String type,
        @NotNull(message = "Start date is required") Instant startDate,
        @NotNull(message = "End date is required") Instant endDate,
        String location) {
}
