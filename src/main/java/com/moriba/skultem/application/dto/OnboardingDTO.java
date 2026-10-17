package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OnboardingDTO(
        String id,
        String leadId,
        String leadName,
        String schoolName,
        // The school on the platform this onboarding is for, once it exists.
        String schoolId,
        String linkedSchoolName,
        String assignedToId,
        String assignedToName,
        String status,
        Instant startedAt,
        Instant completedAt,
        int doneSteps,
        int totalSteps,
        int percent,
        String currentStep,
        LocalDate nextDueDate,
        int overdueSteps,
        List<Step> steps) {

    public record Step(String id, String key, String label, int position, boolean applicable, String status,
            String assignedToId, String assignedToName, LocalDate dueDate, String note, Instant completedAt,
            String completedByName, boolean overdue) {
    }
}
