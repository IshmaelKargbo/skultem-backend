package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "onboarding_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingStepEntity {
    public enum Status {
        PENDING, IN_PROGRESS, DONE
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID onboardingId;

    // One of the standard steps (see OnboardingService.STANDARD_STEPS) or CUSTOM.
    @Column(nullable = false)
    private String stepKey;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private int position;

    // A step that does not apply to this school is switched off, not deleted.
    @Column(nullable = false)
    private boolean applicable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String assignedTo;

    private LocalDate dueDate;

    @Column(columnDefinition = "text")
    private String note;

    private Instant completedAt;

    private String completedBy;
}
