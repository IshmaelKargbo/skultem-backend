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

// A blog category (e.g. "Fees", "School tips"): a post sits in at most one, and the marketing site
// lists and filters posts by them.
@Entity
@Table(name = "marketing_blog_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogCategoryEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private int displayOrder;

    private Instant createdAt;

    private Instant updatedAt;
}
