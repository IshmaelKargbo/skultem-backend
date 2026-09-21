package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveNationalCalendarDTO(
        @NotBlank(message = "Academic year name is required") String name,
        @NotNull(message = "Start date is required") LocalDate startDate,
        @NotNull(message = "End date is required") LocalDate endDate,
        @NotEmpty(message = "At least one term is required")
        @Size(max = 3, message = "An academic year can have at most 3 terms")
        @Valid List<Term> terms) {

    public record Term(
            @NotBlank(message = "Term name is required") String name,
            @NotNull(message = "Term start date is required") LocalDate startDate,
            @NotNull(message = "Term end date is required") LocalDate endDate) {
    }
}
