package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "marketing_blog_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPostEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(length = 1000)
    private String excerpt;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    private String coverImageUrl;

    // Photo URLs, one per line; the first is the cover. See BlogPostService.
    @Column(columnDefinition = "text")
    private String images;

    private String authorName;

    // Comma separated, e.g. "fees,tips".
    @Column(length = 500)
    private String tags;

    @Column(nullable = false)
    private boolean published;

    private Instant publishedAt;

    private Instant createdAt;

    private Instant updatedAt;
}
