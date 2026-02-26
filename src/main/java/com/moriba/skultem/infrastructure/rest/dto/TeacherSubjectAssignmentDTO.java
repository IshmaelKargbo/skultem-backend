package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

public record TeacherSubjectAssignmentDTO(
                @NotNull(message = "Subject is required") String subjectId,
                @NotNull(message = "Teacher is required") String teacherId) {
}
