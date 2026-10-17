package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class SupportDTO {

    private SupportDTO() {
    }

    public record Ticket(String id, String number, String schoolId, String schoolName, String requesterName,
            String requesterEmail, String requesterPhone, String category, String priority, String status,
            String subject, String description, String assignedToId, String assignedToName, String resolutionNote,
            String createdByName, Instant createdAt, Instant updatedAt, Instant resolvedAt,
            // Still open (not resolved or closed) and nobody has it.
            boolean unassigned,
            // The statuses it can be moved to from where it is.
            List<String> nextStatuses) {
    }

    public record Message(String id, String kind, String body, String createdByName, Instant createdAt) {
    }

    public record Detail(Ticket ticket, List<Message> messages) {
    }

    public record Summary(long open, long inProgress, long waiting, long resolved, long unassigned, long urgent,
            Map<String, Long> byStatus) {
    }
}
