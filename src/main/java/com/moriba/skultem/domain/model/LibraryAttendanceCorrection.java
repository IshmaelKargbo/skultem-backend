package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.model.LibrarySessionRecord.Status;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/**
 * A change to a finalized library session mark: what it was, what it became, who changed it, when and why.
 * Never changed or removed - the record row shows the current mark, these show how it got there.
 */
@Getter
public class LibraryAttendanceCorrection extends AggregateRoot<String> {

    private final String schoolId;
    private final String sessionId;
    private final String recordId;
    private final String studentId;
    private final String studentName;
    private final Status originalStatus;
    private final Status correctedStatus;
    private final String reason;
    private final Instant correctedAt;
    private final String correctedBy;
    private final String correctedByName;

    public LibraryAttendanceCorrection(String id, String schoolId, String sessionId, String recordId,
            String studentId, String studentName, Status originalStatus, Status correctedStatus, String reason,
            Instant correctedAt, String correctedBy, String correctedByName, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.sessionId = sessionId;
        this.recordId = recordId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.originalStatus = originalStatus;
        this.correctedStatus = correctedStatus;
        this.reason = reason;
        this.correctedAt = correctedAt;
        this.correctedBy = correctedBy;
        this.correctedByName = correctedByName;
        touch(updatedAt);
    }

    public static LibraryAttendanceCorrection of(LibrarySessionRecord record, Status original, String reason,
            String userId, String userName, Instant now) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required for a correction");
        }
        return new LibraryAttendanceCorrection(UUID.randomUUID().toString(), record.getSchoolId(),
                record.getSessionId(), record.getId(), record.getStudentId(), record.getStudentName(), original,
                record.getStatus(), reason.trim(), now, userId, userName, now, now);
    }
}
