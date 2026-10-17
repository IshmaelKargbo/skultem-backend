package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "platform_announcements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformAnnouncementEntity {
    public enum Severity {
        INFO, WARNING, CRITICAL
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(nullable = false)
    private boolean active;

    // Shown from this moment (null = straight away) until endsAt (null = until switched off).
    private Instant startsAt;

    private Instant endsAt;

    private String createdBy;

    private Instant createdAt;

    private Instant updatedAt;
}
