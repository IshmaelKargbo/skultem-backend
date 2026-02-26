package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttendanceRecordDTO(
                @NotBlank(message = "Student is required") String studentId,
                @NotNull(message = "Present is required") Boolean present,
                @NotNull(message = "Excused is required") Boolean excused,
                @NotNull(message = "Late is required") Boolean late,
                String reason) {
}
