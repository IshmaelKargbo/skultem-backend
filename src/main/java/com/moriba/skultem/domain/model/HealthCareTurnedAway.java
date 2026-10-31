package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

/**
 * A visit a "Refuse" limit rule stopped (docs/HEALTH_CARE.md L-5). Kept so the school can see how often its limit
 * turns students away. It isn't a visit and never counts towards a limit. The rule is copied as it was.
 */
@Getter
public class HealthCareTurnedAway {
    private final String id;
    private final String schoolId;
    private final String studentId;
    private final String studentName;
    private final String studentClass;
    private final Instant at;
    private final String complaints;
    private final String ruleName;
    private final int maxVisits;
    private final String period;
    private final int visitCount;
    private final String recordedBy;
    private final String recordedByName;
    private final Instant createdAt;

    public HealthCareTurnedAway(String id, String schoolId, String studentId, String studentName, String studentClass,
            Instant at, String complaints, String ruleName, int maxVisits, String period, int visitCount,
            String recordedBy, String recordedByName, Instant createdAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentClass = studentClass;
        this.at = at;
        this.complaints = complaints;
        this.ruleName = ruleName;
        this.maxVisits = maxVisits;
        this.period = period;
        this.visitCount = visitCount;
        this.recordedBy = recordedBy;
        this.recordedByName = recordedByName;
        this.createdAt = createdAt;
    }

    public static HealthCareTurnedAway record(String schoolId, String studentId, String studentName,
            String studentClass, String complaints, String ruleName, int maxVisits, String period, int visitCount,
            String userId, String userName, Instant now) {
        return new HealthCareTurnedAway(UUID.randomUUID().toString(), schoolId, studentId, studentName, studentClass,
                now, complaints, ruleName, maxVisits, period, visitCount, userId, userName, now);
    }
}
