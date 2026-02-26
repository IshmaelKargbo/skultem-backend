package com.moriba.skultem.application.dto;

import jakarta.validation.constraints.NotNull;

public record AssignTeacherDTO(
        @NotNull(message = "Teacher id are required") String teacherId,
        @NotNull(message = "Section id are required") String sectionId,
        String streamId) {

}
