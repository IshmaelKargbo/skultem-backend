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
@Table(name = "marketing_testimonials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestimonialEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String authorName;

    private String authorRole;

    private String schoolName;

    @Column(nullable = false, columnDefinition = "text")
    private String quote;

    @Column(nullable = false)
    private int rating;

    private String photoUrl;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private int displayOrder;

    private Instant createdAt;

    private Instant updatedAt;
}
