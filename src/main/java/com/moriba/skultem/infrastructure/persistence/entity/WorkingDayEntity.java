package com.moriba.skultem.infrastructure.persistence.entity;

import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.model.WorkingDay;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "working_days")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkingDayEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timing_id", nullable = false)
    private TimingEntity timing;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkingDay.Day day;

    private boolean state;

    private Instant createdAt;
    private Instant updatedAt;
}
