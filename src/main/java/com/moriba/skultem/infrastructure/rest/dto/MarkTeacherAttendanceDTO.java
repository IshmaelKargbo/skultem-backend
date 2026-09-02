package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record MarkTeacherAttendanceDTO(

        @NotNull(message = "Date is required")
        LocalDate date,

        @NotEmpty(message = "At least one record is required")
        @Valid
        List<TeacherAttendanceRecordDTO> records

) {
}
