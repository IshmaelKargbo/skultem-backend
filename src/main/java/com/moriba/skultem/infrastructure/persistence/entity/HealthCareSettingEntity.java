package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "health_care_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthCareSettingEntity {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String schoolId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String limitRules;

    private boolean medicationAllowed;
    private Integer sameMedicineHours;

    @Column(nullable = false, length = 20)
    private String sameMedicineAction;

    private Integer maxMedicationLines;
    private boolean requireBroughtBy;
    private boolean requireTemperature;
    private boolean requireTreatment;
    private boolean requireNotesOnReturn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String notifyOutcomes;

    private boolean allowManualNotify;
    private boolean notifyPrincipalOnReferral;
    private Integer principalAlertVisits;

    @Column(length = 20)
    private String principalAlertPeriod;

    private Integer longStayMinutes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String complaints;

    private String updatedBy;
    private String updatedByName;
    private Instant createdAt;
    private Instant updatedAt;
}
