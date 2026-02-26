package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateHolidayDTO(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Kind is required") String kind,
        @NotNull(message = "Date is required")
        
        @FutureOrPresent(message = "Date cannot be in the past")
        LocalDate date,

        @NotNull(message = "Fixed is required") Boolean fixed) {
}
