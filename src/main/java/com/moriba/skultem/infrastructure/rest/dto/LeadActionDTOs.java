package com.moriba.skultem.infrastructure.rest.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// The small request bodies for acting on a lead.
public final class LeadActionDTOs {

    private LeadActionDTOs() {
    }

    public record ChangeStatus(
            @NotBlank(message = "Status is required") @Pattern(regexp = "NEW|CONTACTED|DEMO_SCHEDULED|DEMO_COMPLETED|INTERESTED|LOST", message = "Unknown status") String status,
            @Size(max = 2000) String reason) {
    }

    public record Demo(
            @NotBlank(message = "Demo status is required") @Pattern(regexp = "SCHEDULED|COMPLETED|CANCELLED", message = "Unknown demo status") String status,
            Instant at) {
    }

    public record AddNote(
            @NotBlank(message = "Kind is required") @Pattern(regexp = "NOTE|CALL|EMAIL|MEETING", message = "Unknown kind") String kind,
            @NotBlank(message = "Write something first") @Size(max = 5000, message = "Note must be at most 5000 characters") String body) {
    }

    public record Convert(@NotBlank(message = "Choose the school this lead became") String schoolId) {
    }
}
