package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.shared.SchoolTimeZone;

import lombok.Getter;

/**
 * One time a student was seen in the sick bay (docs/HEALTH_CARE.md §5).
 * <p>
 * OPEN while the student is there; CLOSED with an outcome; or VOIDED if it was recorded by mistake. A closed
 * visit is never edited - later information goes into append-only follow-up notes. Nothing is closed
 * automatically: an open visit still open after the student's school day ended needs following up, and closing
 * it then records no departure time (§5.4).
 * <p>
 * The student's name and class are copied in when the visit starts, as are the complaints (with their labels and
 * whether they were urgent), so the record reads the same after a class change or a change to the school's list.
 */
@Getter
public class HealthCareVisit extends AggregateRoot<String> {

    public enum Status {
        OPEN, CLOSED, VOIDED
    }

    public enum Outcome {
        RETURNED_TO_CLASS, SENT_HOME, REFERRED
    }

    /** A complaint as it was chosen. */
    public record Complaint(String code, String label, boolean urgent) {
    }

    public record Medication(String id, String name, String dose, Instant givenAt, String recordedBy,
            String recordedByName, String repeatReason) {
    }

    public record Note(String id, String text, Instant at, String by, String byName) {
    }

    public record ParentNotified(String userId, String name, Instant at, boolean manual) {
    }

    /** The limit rule this visit was let past, as it was then (L-7). {@code reason} is null for urgent or warn-only. */
    public record OverLimit(String ruleName, int maxVisits, String period, String action, long count, boolean urgent,
            String reason, String by, String byName) {
    }

    /** What's needed to close a visit. */
    public record Closing(Outcome outcome, Instant leftAt, String treatment, String collectedBy,
            String collectedByRelation, String referredTo, Boolean ambulanceCalled, String accompaniedBy,
            String notes) {
    }

    private final String schoolId;
    private final String studentId;
    private final String studentName;
    private final String studentClass;
    private Status status;
    private final Instant arrivedAt;
    private final Instant schoolDayEndsAt;
    private List<Complaint> complaints;
    private String complaintOther;
    private String broughtBy;
    private BigDecimal temperature;
    private String notes;
    private String treatment;
    private List<Medication> medications;
    private final String continuesVisitId;
    private final OverLimit overLimit;
    private final String openedBy;
    private final String openedByName;
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
    private String followUpReason;
    private List<Note> followUpNotes;
    private List<ParentNotified> parentNotifications;
    private boolean parentsNotifiedByHand;
    private String notifyProblem;
    private Instant voidedAt;
    private String voidedBy;
    private String voidedByName;
    private String voidReason;

    public HealthCareVisit(String id, String schoolId, String studentId, String studentName, String studentClass,
            Status status, Instant arrivedAt, Instant schoolDayEndsAt, List<Complaint> complaints,
            String complaintOther, String broughtBy, BigDecimal temperature, String notes, String treatment,
            List<Medication> medications, String continuesVisitId, OverLimit overLimit, String openedBy,
            String openedByName, Outcome outcome, Instant leftAt, String collectedBy, String collectedByRelation,
            String referredTo, Boolean ambulanceCalled, String accompaniedBy, Instant closedAt, String closedBy,
            String closedByName, String followUpReason, List<Note> followUpNotes,
            List<ParentNotified> parentNotifications, boolean parentsNotifiedByHand, String notifyProblem,
            Instant voidedAt, String voidedBy, String voidedByName, String voidReason, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentClass = studentClass;
        this.status = status;
        this.arrivedAt = arrivedAt;
        this.schoolDayEndsAt = schoolDayEndsAt;
        this.complaints = List.copyOf(complaints);
        this.complaintOther = complaintOther;
        this.broughtBy = broughtBy;
        this.temperature = temperature;
        this.notes = notes;
        this.treatment = treatment;
        this.medications = new ArrayList<>(medications == null ? List.of() : medications);
        this.continuesVisitId = continuesVisitId;
        this.overLimit = overLimit;
        this.openedBy = openedBy;
        this.openedByName = openedByName;
        this.outcome = outcome;
        this.leftAt = leftAt;
        this.collectedBy = collectedBy;
        this.collectedByRelation = collectedByRelation;
        this.referredTo = referredTo;
        this.ambulanceCalled = ambulanceCalled;
        this.accompaniedBy = accompaniedBy;
        this.closedAt = closedAt;
        this.closedBy = closedBy;
        this.closedByName = closedByName;
        this.followUpReason = followUpReason;
        this.followUpNotes = new ArrayList<>(followUpNotes == null ? List.of() : followUpNotes);
        this.parentNotifications = new ArrayList<>(parentNotifications == null ? List.of() : parentNotifications);
        this.parentsNotifiedByHand = parentsNotifiedByHand;
        this.notifyProblem = notifyProblem;
        this.voidedAt = voidedAt;
        this.voidedBy = voidedBy;
        this.voidedByName = voidedByName;
        this.voidReason = voidReason;
        touch(updatedAt);
    }

    /**
     * Starts a visit. Arrival is today and not in the future (the nurse may log it after helping the student),
     * there's at least one complaint, and "Other" carries a description.
     */
    public static HealthCareVisit start(String schoolId, String studentId, String studentName, String studentClass,
            Instant arrivedAt, Instant schoolDayEndsAt, List<Complaint> complaints, String complaintOther,
            String broughtBy, BigDecimal temperature, String notes, String continuesVisitId, OverLimit overLimit,
            String userId, String userName, Instant now) {
        Instant arrived = arrivedAt == null ? now : arrivedAt;
        if (arrived.isAfter(now.plusSeconds(60))) {
            throw new IllegalArgumentException("The arrival time can't be in the future");
        }
        if (!day(arrived).equals(day(now))) {
            throw new IllegalArgumentException("A visit can only be recorded for today");
        }
        requireComplaints(complaints, complaintOther);
        requireTemperature(temperature);
        return new HealthCareVisit(UUID.randomUUID().toString(), schoolId, studentId, studentName, studentClass,
                Status.OPEN, arrived, schoolDayEndsAt, complaints, trim(complaintOther), trim(broughtBy), temperature,
                trim(notes), null, List.of(), continuesVisitId, overLimit, userId, userName, null, null, null, null,
                null, null, null, null, null, null, null, List.of(), List.of(), false, null, null, null, null, null,
                now, now);
    }

    private static void requireComplaints(List<Complaint> complaints, String other) {
        if (complaints == null || complaints.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one complaint");
        }
        boolean hasOther = complaints.stream().anyMatch(c -> HealthCareComplaint.OTHER.equals(c.code()));
        if (hasOther && (other == null || other.isBlank())) {
            throw new IllegalArgumentException("Describe the complaint when you choose \"Other\"");
        }
    }

    private static void requireTemperature(BigDecimal temperature) {
        if (temperature != null && (temperature.compareTo(new BigDecimal("30.0")) < 0
                || temperature.compareTo(new BigDecimal("45.0")) > 0)) {
            throw new IllegalArgumentException("Temperature must be between 30.0 and 45.0 °C");
        }
    }

    private static LocalDate day(Instant instant) {
        return LocalDate.ofInstant(instant, SchoolTimeZone.ZONE);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public LocalDate arrivalDate() {
        return day(arrivedAt);
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public boolean isClosed() {
        return status == Status.CLOSED;
    }

    public boolean isVoided() {
        return status == Status.VOIDED;
    }

    /** Counts towards limits: not voided, and not a continuation of an earlier visit (L-1, L-3). */
    public boolean countsTowardsLimits() {
        return !isVoided() && continuesVisitId == null;
    }

    public boolean isUrgent() {
        return complaints.stream().anyMatch(Complaint::urgent);
    }

    /** Still open after the student's school day ended - someone needs to follow it up (§5.4). */
    public boolean needsFollowUp(Instant now) {
        return isOpen() && !now.isBefore(schoolDayEndsAt);
    }

    /** Minutes in the sick bay: so far while open, or until departure once closed. Null if no departure time. */
    public Long minutesIn(Instant now) {
        if (isOpen()) {
            return Math.max(0, Duration.between(arrivedAt, now).toMinutes());
        }
        return leftAt == null ? null : Duration.between(arrivedAt, leftAt).toMinutes();
    }

    private void requireOpen() {
        if (status == Status.CLOSED) {
            throw new IllegalStateException("This visit is closed. Add a follow-up note instead.");
        }
        if (status == Status.VOIDED) {
            throw new IllegalStateException("This visit was voided");
        }
    }

    /** Changes the details while the student is still in the sick bay. */
    public void update(List<Complaint> complaints, String complaintOther, String broughtBy, BigDecimal temperature,
            String notes, String treatment, Instant now) {
        requireOpen();
        requireComplaints(complaints, complaintOther);
        requireTemperature(temperature);
        this.complaints = List.copyOf(complaints);
        this.complaintOther = trim(complaintOther);
        this.broughtBy = trim(broughtBy);
        this.temperature = temperature;
        this.notes = trim(notes);
        this.treatment = trim(treatment);
        touch(now);
    }

    public Medication addMedication(String name, String dose, Instant givenAt, String repeatReason, String userId,
            String userName, Instant now) {
        requireOpen();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Enter the medicine's name");
        }
        if (dose == null || dose.isBlank()) {
            throw new IllegalArgumentException("Enter the dose given");
        }
        Instant given = givenAt == null ? now : givenAt;
        if (given.isBefore(arrivedAt) || given.isAfter(now.plusSeconds(60))) {
            throw new IllegalArgumentException("The time given must be between the student's arrival and now");
        }
        var line = new Medication(UUID.randomUUID().toString(), name.trim(), dose.trim(), given, userId, userName,
                trim(repeatReason));
        medications.add(line);
        touch(now);
        return line;
    }

    /** Takes off a line entered by mistake - only while the visit is open. */
    public void removeMedication(String medicationId, Instant now) {
        requireOpen();
        if (!medications.removeIf(m -> m.id().equals(medicationId))) {
            throw new IllegalArgumentException("That medication line isn't on this visit");
        }
        touch(now);
    }

    /**
     * Closes the visit with an outcome (§5.3). A visit whose school day is over is followed up instead
     * ({@link #followUp}), since a departure time would be a guess.
     */
    public void close(Closing c, boolean treatmentRequired, boolean notesRequiredOnReturn, String userId,
            String userName, Instant now) {
        requireOpen();
        if (needsFollowUp(now)) {
            throw new IllegalStateException(
                    "The school day is over for this visit. Follow it up instead - no departure time is recorded.");
        }
        Instant left = c.leftAt() == null ? now : c.leftAt();
        if (left.isBefore(arrivedAt)) {
            throw new IllegalArgumentException("The departure time can't be before the student arrived");
        }
        if (left.isAfter(now.plusSeconds(60))) {
            throw new IllegalArgumentException("The departure time can't be in the future");
        }
        String treatmentGiven = trim(c.treatment()) != null ? trim(c.treatment()) : this.treatment;
        if (treatmentRequired && treatmentGiven == null) {
            throw new IllegalArgumentException("Record the treatment given before closing the visit");
        }
        if (notesRequiredOnReturn && c.outcome() == Outcome.RETURNED_TO_CLASS && trim(c.notes()) == null
                && this.notes == null) {
            throw new IllegalArgumentException("Add a note before sending the student back to class");
        }
        applyOutcome(c);
        this.treatment = treatmentGiven;
        if (trim(c.notes()) != null) {
            this.notes = trim(c.notes());
        }
        this.leftAt = left;
        finish(userId, userName, now);
    }

    /** Closes a visit nobody closed before the school day ended (§5.4). A reason is required; no departure time. */
    public void followUp(Closing c, String reason, String userId, String userName, Instant now) {
        requireOpen();
        if (!needsFollowUp(now)) {
            throw new IllegalStateException("This visit can be followed up once the school day has ended. Until then, close it.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required");
        }
        applyOutcome(c);
        if (trim(c.treatment()) != null) {
            this.treatment = trim(c.treatment());
        }
        this.followUpReason = reason.trim();
        this.leftAt = null;
        finish(userId, userName, now);
    }

    private void applyOutcome(Closing c) {
        if (c.outcome() == null) {
            throw new IllegalArgumentException("Choose how the visit ended");
        }
        if (c.outcome() == Outcome.SENT_HOME && (trim(c.collectedBy()) == null || trim(c.collectedByRelation()) == null)) {
            throw new IllegalArgumentException("Record who collected the student and their relationship to them");
        }
        if (c.outcome() == Outcome.REFERRED && trim(c.referredTo()) == null) {
            throw new IllegalArgumentException("Record the hospital or clinic the student was referred to");
        }
        this.outcome = c.outcome();
        boolean home = c.outcome() == Outcome.SENT_HOME;
        boolean referred = c.outcome() == Outcome.REFERRED;
        this.collectedBy = home ? trim(c.collectedBy()) : null;
        this.collectedByRelation = home ? trim(c.collectedByRelation()) : null;
        this.referredTo = referred ? trim(c.referredTo()) : null;
        this.ambulanceCalled = referred ? Boolean.TRUE.equals(c.ambulanceCalled()) : null;
        this.accompaniedBy = referred ? trim(c.accompaniedBy()) : null;
    }

    private void finish(String userId, String userName, Instant now) {
        this.status = Status.CLOSED;
        this.closedAt = now;
        this.closedBy = userId;
        this.closedByName = userName;
        touch(now);
    }

    /** Adds a follow-up note to a closed visit (V-8). Notes are never changed or removed. */
    public Note addNote(String text, String userId, String userName, Instant now) {
        if (status != Status.CLOSED) {
            throw new IllegalStateException(status == Status.OPEN
                    ? "While the visit is open, use its notes. Follow-up notes are for closed visits."
                    : "This visit was voided");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Write the note first");
        }
        if (text.trim().length() > 2000) {
            throw new IllegalArgumentException("A note can be at most 2000 characters");
        }
        var note = new Note(UUID.randomUUID().toString(), text.trim(), now, userId, userName);
        followUpNotes.add(note);
        touch(now);
        return note;
    }

    /** Marks a visit recorded by mistake (V-10). It stays on record; it can't be restored. */
    public void voidVisit(String reason, String userId, String userName, Instant now) {
        if (isVoided()) {
            throw new IllegalStateException("This visit is already voided");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required");
        }
        this.status = Status.VOIDED;
        this.voidReason = reason.trim();
        this.voidedAt = now;
        this.voidedBy = userId;
        this.voidedByName = userName;
        touch(now);
    }

    /** Whether the nurse may still notify parents by hand (N-2, N-6): closed, and not done by hand before. */
    public void requireManualNotifyAllowed() {
        if (status != Status.CLOSED) {
            throw new IllegalStateException(isOpen() ? "Close the visit before notifying parents"
                    : "This visit was voided");
        }
        // The last attempt reached nobody (no account, or sending failed): trying again is always allowed.
        if (notifyProblem != null) {
            return;
        }
        if (parentsNotifiedByHand || !parentNotifications.isEmpty()) {
            throw new IllegalStateException("Parents have already been notified about this visit");
        }
    }

    /** A retry after the last attempt reached nobody - allowed even where by-hand notifying is switched off. */
    public boolean isNotifyRetry() {
        return notifyProblem != null;
    }

    /**
     * Records who was told. {@code problem} is NONE when no parent has an account, FAILED when sending failed,
     * or null when everyone was reached.
     */
    public void recordNotified(List<ParentNotified> notified, boolean manual, String problem, Instant now) {
        parentNotifications.addAll(notified);
        if (manual) {
            this.parentsNotifiedByHand = true;
        }
        this.notifyProblem = problem;
        touch(now);
    }
}
