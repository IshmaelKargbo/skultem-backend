package com.moriba.skultem.infrastructure.rest.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

public record CreateNoticeDTO(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Content is required") String content,
        @NotBlank(message = "Category is required") String category,
        @NotBlank(message = "Audience is required") String audience,
        Instant expiresAt,
        // For a notice about something that happens on a date (category EVENT, or any notice that names one).
        Instant eventAt,
        Instant eventEndsAt,
        @jakarta.validation.constraints.Size(max = 255, message = "Location must not exceed 255 characters") String eventLocation,
        // Also put it on the school calendar (needs eventAt); kept in step with the notice afterwards.
        Boolean addToCalendar,
        // Which management section it's for; left out = the caller's own section (or the whole school for
        // whole-school staff). Ignored in a school without sections.
        String managementSectionId) {
}
