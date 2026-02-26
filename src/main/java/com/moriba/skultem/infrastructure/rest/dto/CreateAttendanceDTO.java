package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateAttendanceDTO(
        @NotBlank(message = "Class is required") String classId,
        @NotNull(message = "Date is required") LocalDate date,
        @NotNull(message = "Section is required") String sectionId,
        @NotNull(message = "Mark on holiday is required") boolean markOnHoliday,
        String streamId,
        @NotNull(message = "Assignments are required") @NotEmpty(message = "Assignments cannot be empty") List<AttendanceRecordDTO> records) {
}
