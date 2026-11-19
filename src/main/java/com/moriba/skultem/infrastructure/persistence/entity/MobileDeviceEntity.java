package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import com.moriba.skultem.domain.model.MobileDevice;

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
@Table(name = "mobile_devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobileDeviceEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String userId;
    private String schoolId;
    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String token;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MobileDevice.Platform platform;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant lastUsedAt;
}
