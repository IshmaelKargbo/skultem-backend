package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record LeadDTO(
        String id,
        String name,
        String schoolName,
        String email,
        String phone,
        String city,
        String source,
        String demoRequestId,
        String contactMessageId,
        String status,
        String demoStatus,
        Instant demoAt,
        String assignedToId,
        String assignedToName,
        Instant nextFollowUpAt,
        String summary,
        // Set once the lead became a school on the platform.
        String schoolId,
        String convertedSchoolName,
        Instant convertedAt,
        String lostReason,
        // Still being worked and a follow-up is due (or it has never been contacted).
        boolean awaitingFollowUp,
        String onboardingId,
        // The statuses the lead can be moved to by hand from where it is now.
        List<String> nextStatuses,
        Instant createdAt,
        Instant updatedAt) {

    public record Note(String id, String kind, String body, String createdById, String createdByName,
            Instant createdAt) {
    }

    public record Detail(LeadDTO lead, List<Note> notes) {
    }

    public record Summary(long total, Map<String, Long> byStatus, long awaitingFollowUp, long unassigned,
            long overdue) {
    }

    // A person who can be given a lead (an active system admin).
    public record Staff(String id, String name, String email) {
    }
}
