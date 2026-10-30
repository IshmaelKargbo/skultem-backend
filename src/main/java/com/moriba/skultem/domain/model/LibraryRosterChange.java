package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/** A student added to or taken off a library session's roster before it was finalized. Never changed afterwards. */
@Getter
public class LibraryRosterChange extends AggregateRoot<String> {

    public enum Change {
        ADDED, REMOVED
    }

    private final String schoolId;
    private final String sessionId;
    private final String studentId;
    private final String studentName;
    private final Change change;
    private final String reason;
    private final Instant changedAt;
    private final String changedBy;
    private final String changedByName;

    public LibraryRosterChange(String id, String schoolId, String sessionId, String studentId, String studentName,
            Change change, String reason, Instant changedAt, String changedBy, String changedByName,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.change = change;
        this.reason = reason;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
        this.changedByName = changedByName;
        touch(updatedAt);
    }

    public static LibraryRosterChange record(String schoolId, String sessionId, String studentId, String studentName,
            Change change, String reason, String userId, String userName, Instant now) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required");
        }
        return new LibraryRosterChange(UUID.randomUUID().toString(), schoolId, sessionId, studentId, studentName,
                change, reason.trim(), now, userId, userName, now, now);
    }
}
