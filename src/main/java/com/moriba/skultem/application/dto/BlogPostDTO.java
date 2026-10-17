package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

// content is null in list views (BlogPostService.summary) - only the single-post read carries the body.
public record BlogPostDTO(String id, String title, String slug, String excerpt, String content,
        String coverImageUrl, List<String> images, String authorName, List<String> tags, boolean published, Instant publishedAt,
        Instant createdAt, Instant updatedAt) {
}
