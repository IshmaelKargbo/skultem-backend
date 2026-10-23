package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveMarketingVideoDTO(
        @NotBlank(message = "Title is required") @Size(max = 255, message = "Title must be at most 255 characters") String title,
        @Size(max = 1000, message = "Description must be at most 1000 characters") String description,
        // Any YouTube link - youtube.com/watch?v=..., youtu.be/..., /shorts/..., /embed/... - or the bare video id.
        @NotBlank(message = "YouTube link is required") @Size(max = 500) String youtubeUrl,
        boolean featured,
        boolean published,
        int displayOrder) {
}
