package com.moriba.skultem.application.dto;

import java.util.List;

import com.moriba.skultem.infrastructure.rest.dto.TeacherSubjectAssignmentDTO;

import jakarta.validation.constraints.NotNull;

public record AssignSubjectToTeacherDTO(
                @NotNull(message = "Assignments are required") List<TeacherSubjectAssignmentDTO> assignments) {

}
