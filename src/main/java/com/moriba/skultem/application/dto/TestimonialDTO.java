package com.moriba.skultem.application.dto;

import java.time.Instant;

public record TestimonialDTO(String id, String authorName, String authorRole, String schoolName, String quote,
        int rating, String photoUrl, boolean featured, boolean published, int displayOrder, Instant createdAt,
        Instant updatedAt) {
}
