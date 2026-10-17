package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class SupportRequests {

    private SupportRequests() {
    }

    public record Create(
            // The school it is about, when there is one (a general question may not have one).
            String schoolId,
            @NotBlank(message = "Who is asking?") @Size(max = 255) String requesterName,
            @Email(message = "Invalid email format") @Size(max = 255) String requesterEmail,
            @Pattern(regexp = "^$|^[0-9+ ]{7,20}$", message = "Phone number may only contain digits, spaces and +") String requesterPhone,
            @NotBlank(message = "Choose a category") @Pattern(regexp = "ACCOUNT_ACCESS|BILLING|BUG|TRAINING|DATA|FEATURE_REQUEST|OTHER", message = "Unknown category") String category,
            @NotBlank(message = "Choose a priority") @Pattern(regexp = "LOW|NORMAL|HIGH|URGENT", message = "Unknown priority") String priority,
            @NotBlank(message = "Give the ticket a subject") @Size(max = 255, message = "Subject must be at most 255 characters") String subject,
            @NotBlank(message = "Describe the problem") @Size(max = 10000, message = "Description must be at most 10000 characters") String description,
            String assignedToId) {
    }

    // Anything left null stays as it was.
    public record Update(
            @Pattern(regexp = "ACCOUNT_ACCESS|BILLING|BUG|TRAINING|DATA|FEATURE_REQUEST|OTHER", message = "Unknown category") String category,
            @Pattern(regexp = "LOW|NORMAL|HIGH|URGENT", message = "Unknown priority") String priority,
            String assignedToId,
            Boolean clearAssignee) {
    }

    public record ChangeStatus(
            @NotBlank(message = "Status is required") @Pattern(regexp = "OPEN|IN_PROGRESS|WAITING|RESOLVED|CLOSED", message = "Unknown status") String status,
            @Size(max = 5000) String resolutionNote) {
    }

    public record AddMessage(
            @NotBlank(message = "Kind is required") @Pattern(regexp = "REPLY|FROM_SCHOOL|INTERNAL", message = "Unknown kind") String kind,
            @NotBlank(message = "Write something first") @Size(max = 10000, message = "Message must be at most 10000 characters") String body) {
    }
}
