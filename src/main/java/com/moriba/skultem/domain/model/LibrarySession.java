package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/**
 * One occurrence of a Library period from the school's timetable, once someone has opened or cancelled it
 * (docs/LIBRARY_ATTENDANCE.md, Workflow B).
 * <p>
 * Sessions nobody has touched yet are not stored: they are worked out from the timetable and shown as
 * Upcoming. Opening one saves it, with its own copy of the class, subject, period and room - the
 * timetable entry can be edited or deleted later without changing a session that already happened.
 * <p>
 * IN_PROGRESS -> COMPLETED (finalized, every student marked) or CANCELLED (with a reason). Neither of
 * those can be undone; a finalized session is changed only through corrections.
 */
@Getter
public class LibrarySession extends AggregateRoot<String> {

    public enum Status {
        IN_PROGRESS, COMPLETED, CANCELLED
    }

    /** Where a session comes from: one Library timetable entry on one date, with its details at that time. */
    public record Slot(String timetableEntryId, LocalDate date, String classSessionId, String className,
            String subjectId, String subjectName, String periodName, LocalTime startTime, LocalTime endTime,
            String roomName, String termId, LocalDate termEndDate) {
    }

    private final String schoolId;
    private final String timetableEntryId;
    private final LocalDate date;
    private final String classSessionId;
    private final String className;
    private final String subjectId;
    private final String subjectName;
    private final String periodName;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String roomName;
    private final String termId;
    private final LocalDate termEndDate;
    private Status status;
    private final Instant openedAt;
    private final String openedBy;
    private final String openedByName;
    private Instant finalizedAt;
    private String finalizedBy;
    private String finalizedByName;
    private Instant cancelledAt;
    private String cancelledBy;
    private String cancelledByName;
    private String cancelReason;

    public LibrarySession(String id, String schoolId, String timetableEntryId, LocalDate date, String classSessionId,
            String className, String subjectId, String subjectName, String periodName, LocalTime startTime,
            LocalTime endTime, String roomName, String termId, LocalDate termEndDate, Status status,
            Instant openedAt, String openedBy, String openedByName, Instant finalizedAt, String finalizedBy,
            String finalizedByName, Instant cancelledAt, String cancelledBy, String cancelledByName,
            String cancelReason, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.timetableEntryId = timetableEntryId;
        this.date = date;
        this.classSessionId = classSessionId;
        this.className = className;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.periodName = periodName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.roomName = roomName;
        this.termId = termId;
        this.termEndDate = termEndDate;
        this.status = status;
        this.openedAt = openedAt;
        this.openedBy = openedBy;
        this.openedByName = openedByName;
        this.finalizedAt = finalizedAt;
        this.finalizedBy = finalizedBy;
        this.finalizedByName = finalizedByName;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.cancelledByName = cancelledByName;
        this.cancelReason = cancelReason;
        touch(updatedAt);
    }

    /**
     * Opens a session to take the register. Not before its day (there's nothing to record yet), and not
     * after its term has ended (late entry is allowed until then).
     */
    public static LibrarySession open(String schoolId, Slot slot, String userId, String userName, LocalDate today,
            Instant now) {
        if (slot.date().isAfter(today)) {
            throw new IllegalStateException("This session hasn't happened yet. It can be opened on "
                    + slot.date() + ".");
        }
        requireWithinTerm(slot, today);
        return new LibrarySession(UUID.randomUUID().toString(), schoolId, slot.timetableEntryId(), slot.date(),
                slot.classSessionId(), slot.className(), slot.subjectId(), slot.subjectName(), slot.periodName(),
                slot.startTime(), slot.endTime(), slot.roomName(), slot.termId(), slot.termEndDate(),
                Status.IN_PROGRESS, now, userId, userName, null, null, null, null, null, null, null, now, now);
    }

    /** Cancels a session nobody has opened yet. It is saved, so the cancellation and its reason are kept. */
    public static LibrarySession cancelBeforeOpening(String schoolId, Slot slot, String userId, String userName,
            String reason, LocalDate today, Instant now) {
        requireWithinTerm(slot, today);
        String why = requireReason(reason);
        return new LibrarySession(UUID.randomUUID().toString(), schoolId, slot.timetableEntryId(), slot.date(),
                slot.classSessionId(), slot.className(), slot.subjectId(), slot.subjectName(), slot.periodName(),
                slot.startTime(), slot.endTime(), slot.roomName(), slot.termId(), slot.termEndDate(),
                Status.CANCELLED, null, null, null, null, null, null, now, userId, userName, why, now, now);
    }

    private static void requireWithinTerm(Slot slot, LocalDate today) {
        if (slot.termEndDate() != null && today.isAfter(slot.termEndDate())) {
            throw new IllegalStateException("This session's term has ended, so it can no longer be changed");
        }
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required");
        }
        return reason.trim();
    }

    public boolean isInProgress() {
        return status == Status.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    /** Marking, roster changes and finalizing: only while in progress and before the term ends. */
    public void requireEditable(LocalDate today) {
        if (status == Status.COMPLETED) {
            throw new IllegalStateException(
                    "This session has been finalized. A principal can correct it if something is wrong.");
        }
        if (status == Status.CANCELLED) {
            throw new IllegalStateException("This session was cancelled");
        }
        if (today.isAfter(termEndDate)) {
            throw new IllegalStateException("This session's term has ended, so it can no longer be changed");
        }
    }

    /** The period has ended and the register still isn't finalized. Nobody is marked absent for it. */
    public boolean isIncomplete(LocalDateTime now) {
        return status == Status.IN_PROGRESS && LocalDateTime.of(date, endTime).isBefore(now);
    }

    /** Locks the register. Every student on the roster must have a final mark. */
    public void finalizeWith(List<LibrarySessionRecord> records, String userId, String userName, LocalDate today,
            Instant now) {
        requireEditable(today);
        var counted = records.stream().filter(LibrarySessionRecord::isCounted).toList();
        if (counted.isEmpty()) {
            throw new IllegalStateException("There are no students on this session's roster");
        }
        long unmarked = counted.stream().filter(r -> !r.getStatus().isFinal()).count();
        if (unmarked > 0) {
            throw new IllegalStateException(unmarked == 1
                    ? "1 student is still unmarked. Mark them Present, Absent or Excused before finalizing."
                    : unmarked + " students are still unmarked. Mark each one Present, Absent or Excused before finalizing.");
        }
        this.status = Status.COMPLETED;
        this.finalizedAt = now;
        this.finalizedBy = userId;
        this.finalizedByName = userName;
        touch(now);
    }

    /** Cancels a session in progress. Marks already entered are kept but no longer count. */
    public void cancel(String userId, String userName, String reason, LocalDate today, Instant now) {
        requireEditable(today);
        this.cancelReason = requireReason(reason);
        this.status = Status.CANCELLED;
        this.cancelledAt = now;
        this.cancelledBy = userId;
        this.cancelledByName = userName;
        touch(now);
    }

    public void requireCorrectable() {
        if (status != Status.COMPLETED) {
            throw new IllegalStateException("Only a finalized session can be corrected");
        }
    }
}
