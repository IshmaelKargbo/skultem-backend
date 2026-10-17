package com.moriba.skultem.infrastructure.rest.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SavePlatformAnnouncementDTO(
        @NotBlank(message = "Title is required") @Size(max = 255, message = "Title must be at most 255 characters") String title,
        @NotBlank(message = "Message is required") @Size(max = 2000, message = "Message must be at most 2000 characters") String message,
        @NotBlank(message = "Severity is required") @Pattern(regexp = "INFO|WARNING|CRITICAL", message = "Severity must be INFO, WARNING or CRITICAL") String severity,
        boolean active,
        Instant startsAt,
        Instant endsAt) {
}
