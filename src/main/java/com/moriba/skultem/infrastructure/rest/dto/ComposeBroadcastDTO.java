package com.moriba.skultem.infrastructure.rest.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record ComposeBroadcastDTO(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Message is required") String message,
        @NotBlank(message = "Audience is required") String audience,
        @NotEmpty(message = "Select at least one channel") List<String> channels,
        @NotBlank(message = "Send option is required") String sendOption,
        Instant scheduledAt) {
}
