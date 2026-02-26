package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record MarkClassAttendanceDTO(
        @NotNull(message = "Date is required") LocalDate date,
        @NotNull(message = "Holiday is required") Boolean holiday,
        @NotNull(message = "Attendance records are required")
        @NotEmpty(message = "Attendance records cannot be empty")
        List<@Valid ClassAttendanceMarkDTO> records) {
}
