package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSessionEntity {
    @Id
    private String id;

    private String schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private String device;

    @Column(nullable = false)
    private String deviceType;

    @Column(nullable = false)
    private String os;

    @Column(nullable = false)
    private String browser;

    @Column(nullable = false)
    private String userAgent;

    private boolean active;

    private Instant createdAt;
    private Instant updatedAt;
}
