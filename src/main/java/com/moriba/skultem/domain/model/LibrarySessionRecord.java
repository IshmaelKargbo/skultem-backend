package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/**
 * One student on a scheduled library session's roster, and their mark.
 * <p>
 * Every row starts UNMARKED, a working value only: a session can't be finalized while any row on the
 * roster is still UNMARKED. A student taken off the roster before finalizing is kept as {@code removed}
 * (so the change stays visible) and isn't counted.
 */
@Getter
public class LibrarySessionRecord extends AggregateRoot<String> {

    public enum Status {
        UNMARKED, PRESENT, ABSENT, EXCUSED;

        public boolean isFinal() {
            return this != UNMARKED;
        }
    }

    private final String schoolId;
    private final String sessionId;
    private final String studentId;
    private final String studentName;
    private final String studentClass;
    private Status status;
    private String excuseReason;
    private boolean removed;
    private final boolean addedManually;
    private boolean corrected;
    private Instant markedAt;
    private String markedBy;

    public LibrarySessionRecord(String id, String schoolId, String sessionId, String studentId, String studentName,
            String studentClass, Status status, String excuseReason, boolean removed, boolean addedManually,
            boolean corrected, Instant markedAt, String markedBy, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentClass = studentClass;
        this.status = status;
        this.excuseReason = excuseReason;
        this.removed = removed;
        this.addedManually = addedManually;
        this.corrected = corrected;
        this.markedAt = markedAt;
        this.markedBy = markedBy;
        touch(updatedAt);
    }

    public static LibrarySessionRecord onRoster(String schoolId, String sessionId, String studentId,
            String studentName, String studentClass, boolean addedManually) {
        Instant now = Instant.now();
        return new LibrarySessionRecord(UUID.randomUUID().toString(), schoolId, sessionId, studentId, studentName,
                studentClass, Status.UNMARKED, null, false, addedManually, false, null, null, now, now);
    }

    /** Counts towards the session: on the roster and not taken off it. */
    public boolean isCounted() {
        return !removed;
    }

    /** Marking while the session is in progress. A reason only goes with EXCUSED, and is optional. */
    public void mark(Status status, String excuseReason, String userId, Instant now) {
        if (removed) {
            throw new IllegalStateException(studentName + " has been taken off this session's roster");
        }
        if (status == null || !status.isFinal()) {
            throw new IllegalArgumentException("Mark each student Present, Absent or Excused");
        }
        this.status = status;
        this.excuseReason = status == Status.EXCUSED && excuseReason != null && !excuseReason.isBlank()
                ? excuseReason.trim()
                : null;
        this.markedAt = now;
        this.markedBy = userId;
        touch(now);
    }

    public void removeFromRoster() {
        if (removed) {
            throw new IllegalStateException(studentName + " is not on this session's roster");
        }
        this.removed = true;
        touch(Instant.now());
    }

    /** A student taken off earlier and added back: on the roster again, unmarked. */
    public void restoreToRoster() {
        if (!removed) {
            throw new IllegalStateException(studentName + " is already on this session's roster");
        }
        this.removed = false;
        this.status = Status.UNMARKED;
        this.excuseReason = null;
        this.markedAt = null;
        this.markedBy = null;
        touch(Instant.now());
    }

    /**
     * Changes a finalized mark. Returns the status it had, which the caller keeps on the correction record.
     * The caller has already checked the session is finalized and the user may correct it.
     */
    public Status correct(Status newStatus, String excuseReason, String userId, Instant now) {
        if (removed) {
            throw new IllegalStateException(studentName + " was not on this session's roster");
        }
        if (newStatus == null || !newStatus.isFinal()) {
            throw new IllegalArgumentException("A correction must be Present, Absent or Excused");
        }
        if (newStatus == status) {
            throw new IllegalArgumentException(studentName + " is already marked " + label(status));
        }
        Status original = status;
        this.status = newStatus;
        this.excuseReason = newStatus == Status.EXCUSED && excuseReason != null && !excuseReason.isBlank()
                ? excuseReason.trim()
                : null;
        this.corrected = true;
        this.markedAt = now;
        this.markedBy = userId;
        touch(now);
        return original;
    }

    public static String label(Status status) {
        return switch (status) {
            case UNMARKED -> "Unmarked";
            case PRESENT -> "Present";
            case ABSENT -> "Absent";
            case EXCUSED -> "Excused";
        };
    }
}
