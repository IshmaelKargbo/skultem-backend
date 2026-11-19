package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

// What a teacher did about a pupil flagged as needing attention, and whether that settled it. Records are
// never edited: a new one is added each time something more is done, so the history stays honest.
@Getter
public class StudentFollowUp {
    public enum Action {
        CALLED_GUARDIAN, MESSAGED_GUARDIAN, MET_GUARDIAN, SPOKE_WITH_PUPIL, OTHER
    }

    private final String id;
    private final String schoolId;
    private final String studentId;
    private final String classId;
    private final Action action;
    private final String note;
    private final boolean resolved;
    private final String recordedByUserId;
    private final String recordedByName;
    private final Instant createdAt;

    public StudentFollowUp(String id, String schoolId, String studentId, String classId, Action action, String note,
            boolean resolved, String recordedByUserId, String recordedByName, Instant createdAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.studentId = studentId;
        this.classId = classId;
        this.action = action;
        this.note = note;
        this.resolved = resolved;
        this.recordedByUserId = recordedByUserId;
        this.recordedByName = recordedByName;
        this.createdAt = createdAt;
    }

    public static StudentFollowUp record(String schoolId, String studentId, String classId, Action action,
            String note, boolean resolved, String userId, String userName) {
        return new StudentFollowUp(UUID.randomUUID().toString(), schoolId, studentId, classId, action, note, resolved,
                userId, userName, Instant.now());
    }
}
