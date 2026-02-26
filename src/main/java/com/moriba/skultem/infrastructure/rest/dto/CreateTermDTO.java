package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record CreateTermDTO(
                @NotNull(message = "Name is required") String name,

                @NotNull(message = "Academic year is required") String academicYearId,

                @NotNull(message = "Start date is required") LocalDate startDate,

                @NotNull(message = "End date are required") LocalDate endDate) {
}
