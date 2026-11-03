package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveExamScheduleDTO(
        @NotBlank(message = "Choose a term") String termId,
        @NotBlank(message = "Choose a class") String classId,
        @NotBlank(message = "Choose a subject") String subjectId,
        @Size(max = 100) String paper,
        @NotNull(message = "Choose the exam date") LocalDate examDate,
        @NotNull(message = "Choose a start time") LocalTime startTime,
        @NotNull(message = "Choose an end time") LocalTime endTime,
        @Size(max = 150) String venue,
        String invigilatorId,
        @Size(max = 500) String notes) {
}
