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

// A new school's journey from first demo to launch. One per lead; its steps are OnboardingStepEntity.
@Entity
@Table(name = "onboardings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingEntity {
    public enum Status {
        IN_PROGRESS, COMPLETED, CANCELLED
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID leadId;

    // Set once the school exists on the platform.
    private String schoolId;

    private String assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private Instant startedAt;

    private Instant completedAt;

    private Instant createdAt;

    private Instant updatedAt;
}
