package com.moriba.skultem.infrastructure.rest.dto;

import com.moriba.skultem.domain.model.TeacherAttendance.Status;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TeacherAttendanceRecordDTO(

        @NotBlank(message = "Teacher is required")
        String teacherId,

        @NotNull(message = "Status is required")
        Status status,

        String note

) {
}
