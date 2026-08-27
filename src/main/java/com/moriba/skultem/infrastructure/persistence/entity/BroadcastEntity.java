package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import com.moriba.skultem.domain.model.Broadcast.Status;
import com.moriba.skultem.domain.vo.Audience;

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
@Table(name = "broadcasts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Audience audience;

    @Column(nullable = false)
    private String channels;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private int recipientsCount;

    @Column(nullable = false)
    private int deliveredCount;

    @Column(nullable = false)
    private String sentByUserId;

    @Column(nullable = false)
    private String sentByName;

    private Instant scheduledAt;
    private Instant sentAt;

    private Instant createdAt;
    private Instant updatedAt;
}
