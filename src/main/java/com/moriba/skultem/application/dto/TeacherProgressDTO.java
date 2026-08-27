package com.moriba.skultem.application.dto;

public record TeacherProgressDTO(
        String id,
        String name,
        int subjects,
        int classes,
        int completedWeeks,
        int totalWeeks,
        int coverage,
        String status) {
}
