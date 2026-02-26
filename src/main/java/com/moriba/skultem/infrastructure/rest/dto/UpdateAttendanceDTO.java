package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record UpdateAttendanceDTO(
        @NotNull(message = "Date is required") LocalDate date,
        @NotNull(message = "Present is required") Boolean present,
        @NotNull(message = "Excused is required") Boolean excused,
        @NotNull(message = "Late is required") Boolean late,
        String reason,
        @NotNull(message = "Holiday is required") Boolean holiday) {
}
