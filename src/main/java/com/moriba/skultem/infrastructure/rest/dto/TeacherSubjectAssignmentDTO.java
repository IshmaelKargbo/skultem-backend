package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

public record TeacherSubjectAssignmentDTO(
        String id,
        @NotNull(message = "Subject is required") String subjectId,
        @NotNull(message = "Teacher is required") String teacherId) {
}
