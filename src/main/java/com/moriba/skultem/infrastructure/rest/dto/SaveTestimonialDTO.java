package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveTestimonialDTO(
        @NotBlank(message = "Author name is required") @Size(max = 255) String authorName,
        @Size(max = 255) String authorRole,
        @Size(max = 255) String schoolName,
        @NotBlank(message = "Quote is required") @Size(max = 2000, message = "Quote must be at most 2000 characters") String quote,
        @Min(value = 1, message = "Rating must be between 1 and 5") @Max(value = 5, message = "Rating must be between 1 and 5") int rating,
        @Size(max = 1024) String photoUrl,
        boolean featured,
        boolean published,
        int displayOrder) {
}
