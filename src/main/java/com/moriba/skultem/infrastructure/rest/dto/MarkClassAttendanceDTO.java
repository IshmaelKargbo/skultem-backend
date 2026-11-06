package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record MarkClassAttendanceDTO(
        @NotNull(message = "Date is required") LocalDate date,
        @NotNull(message = "Holiday is required") Boolean holiday,
        // Which roll call (1 or 2) - only needed when the class takes two roll calls a day.
        @Min(value = 1, message = "Roll call must be 1 or 2") @Max(value = 2, message = "Roll call must be 1 or 2") Integer roll,
        @NotNull(message = "Attendance records are required")
        @NotEmpty(message = "Attendance records cannot be empty")
        List<@Valid ClassAttendanceMarkDTO> records) {
}
