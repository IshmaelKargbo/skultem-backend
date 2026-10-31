package com.moriba.skultem.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.moriba.skultem.domain.model.HealthCareVisit.Outcome;
import com.moriba.skultem.domain.model.HealthCareVisit.Status;

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
@Table(name = "health_care_visits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthCareVisitEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String studentName;

    private String studentClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private Instant arrivedAt;

    @Column(nullable = false)
    private Instant schoolDayEndsAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String complaints;

    @Column(nullable = false, length = 1000)
    private String complaintCodes;

    @Column(length = 500)
    private String complaintOther;

    private String broughtBy;

    @Column(precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(length = 2000)
    private String notes;

    @Column(length = 2000)
    private String treatment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String medications;

    private String continuesVisitId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String overLimit;

    @Column(nullable = false)
    private String openedBy;

    private String openedByName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Outcome outcome;

    private Instant leftAt;
    private String collectedBy;
    private String collectedByRelation;
    private String referredTo;
    private Boolean ambulanceCalled;
    private String accompaniedBy;
    private Instant closedAt;
    private String closedBy;
    private String closedByName;

    @Column(length = 500)
    private String followUpReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String followUpNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String parentNotifications;

    private boolean parentsNotifiedByHand;

    @Column(length = 20)
    private String notifyProblem;

    private Instant voidedAt;
    private String voidedBy;
    private String voidedByName;

    @Column(length = 500)
    private String voidReason;

    private Instant createdAt;
    private Instant updatedAt;
}
