package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "push_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushSubscriptionEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String userId;

    private String schoolId;

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String endpoint;

    @Column(nullable = false)
    private String p256dh;

    @Column(nullable = false)
    private String auth;

    @Column(length = 512)
    private String userAgent;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastUsedAt;
}
