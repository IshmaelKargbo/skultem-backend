package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class OnboardingActionDTOs {

    private OnboardingActionDTOs() {
    }

    // Who is responsible for the onboarding, and which school it ended up as.
    public record Update(String assignedToId, String schoolId) {
    }

    // Anything left null is left as it was. clearDueDate / clearAssignee are explicit because null means "keep".
    public record StepUpdate(
            @Pattern(regexp = "PENDING|IN_PROGRESS|DONE", message = "Unknown step status") String status,
            Boolean applicable,
            String assignedToId,
            Boolean clearAssignee,
            LocalDate dueDate,
            Boolean clearDueDate,
            @Size(max = 5000, message = "Note must be at most 5000 characters") String note) {
    }

    public record NewStep(@NotBlank(message = "Name the step") @Size(max = 255, message = "Step name must be at most 255 characters") String label) {
    }
}
