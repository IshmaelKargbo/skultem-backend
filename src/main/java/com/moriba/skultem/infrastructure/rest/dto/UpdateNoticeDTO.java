package com.moriba.skultem.infrastructure.rest.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

public record UpdateNoticeDTO(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Content is required") String content,
        @NotBlank(message = "Category is required") String category,
        @NotBlank(message = "Audience is required") String audience,
        Instant expiresAt) {
}
