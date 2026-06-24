package com.moriba.skultem.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "timings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimingEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private int periodDuration;

    private int breakDuration;

    private int lunchDuration;

    private Instant createdAt;
    private Instant updatedAt;
}
