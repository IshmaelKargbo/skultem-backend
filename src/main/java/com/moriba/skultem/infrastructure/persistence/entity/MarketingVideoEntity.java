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
@Table(name = "marketing_videos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketingVideoEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    // The 11-character YouTube video id - every link and thumbnail is built from it.
    @Column(nullable = false, unique = true, length = 11)
    private String youtubeId;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private int displayOrder;

    private Instant createdAt;

    private Instant updatedAt;
}
