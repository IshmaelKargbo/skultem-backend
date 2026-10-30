package com.moriba.skultem.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/**
 * A student's own visit to the library, outside a scheduled Library period (docs/LIBRARY_ATTENDANCE.md, Workflow A).
 * <p>
 * A visit is OPEN from check-in until it is either checked out or, if the student left without being
 * checked out, RESOLVED by a librarian or principal-level user. A resolution never invents a check-out
 * time - the visit simply has none, and so no duration.
 * <p>
 * The student's name and class are copied in at check-in, so the history keeps reading correctly after
 * a class change or exit.
 */
@Getter
public class LibraryVisit extends AggregateRoot<String> {

    public enum Status {
        OPEN, CHECKED_OUT, RESOLVED
    }

    private final String schoolId;
    private final String studentId;
    private final String studentName;
    private final String studentClass;
    private Status status;
    private final Instant checkedInAt;
    private final String checkedInBy;
    private final String checkedInByName;
    // The end of the student's school day on the check-in date. Still open after this = unresolved.
    private final Instant schoolDayEndsAt;
    private Instant checkedOutAt;
    private String checkedOutBy;
    private String checkedOutByName;
    private Instant resolvedAt;
    private String resolvedBy;
    private String resolvedByName;
    private String resolutionReason;

    public LibraryVisit(String id, String schoolId, String studentId, String studentName, String studentClass,
            Status status, Instant checkedInAt, String checkedInBy, String checkedInByName, Instant schoolDayEndsAt,
            Instant checkedOutAt, String checkedOutBy, String checkedOutByName, Instant resolvedAt,
            String resolvedBy, String resolvedByName, String resolutionReason, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentClass = studentClass;
        this.status = status;
        this.checkedInAt = checkedInAt;
        this.checkedInBy = checkedInBy;
        this.checkedInByName = checkedInByName;
        this.schoolDayEndsAt = schoolDayEndsAt;
        this.checkedOutAt = checkedOutAt;
        this.checkedOutBy = checkedOutBy;
        this.checkedOutByName = checkedOutByName;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
        this.resolvedByName = resolvedByName;
        this.resolutionReason = resolutionReason;
        touch(updatedAt);
    }

    public static LibraryVisit checkIn(String schoolId, String studentId, String studentName, String studentClass,
            String userId, String userName, Instant now, Instant schoolDayEndsAt) {
        if (schoolDayEndsAt == null) {
            throw new IllegalArgumentException("The end of the school day is required");
        }
        return new LibraryVisit(UUID.randomUUID().toString(), schoolId, studentId, studentName, studentClass,
                Status.OPEN, now, userId, userName, schoolDayEndsAt, null, null, null, null, null, null, null,
                now, now);
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    /** Still open after the student's school day ended - someone needs to follow it up. */
    public boolean isUnresolved(Instant now) {
        return isOpen() && !now.isBefore(schoolDayEndsAt);
    }

    public void checkOut(String userId, String userName, Instant now) {
        if (!isOpen()) {
            throw new IllegalStateException(studentName + " is not checked in to the library");
        }
        // Clock differences between servers must never produce a negative visit.
        this.checkedOutAt = now.isBefore(checkedInAt) ? checkedInAt : now;
        this.checkedOutBy = userId;
        this.checkedOutByName = userName;
        this.status = Status.CHECKED_OUT;
        touch(now);
    }

    /**
     * Closes a visit the student left without being checked out. Only once their school day has ended -
     * before that, check them out instead. The missing check-out is kept as missing.
     */
    public void resolve(String userId, String userName, String reason, Instant now) {
        if (!isOpen()) {
            throw new IllegalStateException("This visit is already closed");
        }
        if (!isUnresolved(now)) {
            throw new IllegalStateException(
                    "This visit can be followed up once the school day has ended. Until then, check the student out.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required");
        }
        this.resolvedAt = now;
        this.resolvedBy = userId;
        this.resolvedByName = userName;
        this.resolutionReason = reason.trim();
        this.status = Status.RESOLVED;
        touch(now);
    }

    /** Minutes in the library - only for a visit closed by a real check-out, otherwise null. */
    public Long durationMinutes() {
        if (status != Status.CHECKED_OUT) {
            return null;
        }
        return Duration.between(checkedInAt, checkedOutAt).toMinutes();
    }
}
