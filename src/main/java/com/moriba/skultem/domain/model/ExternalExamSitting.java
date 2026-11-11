package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.List;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/**
 * A class sitting an external exam in an academic year ("JSS 3 sits BECE, 2026/2027"): the subjects the exam
 * covers, and its candidates (ExternalExamCandidate, the class's students). Scores are entered while it is a DRAFT;
 * CONFIRMED locks them, and only then are result slips and statements printed. Reopening unlocks it to correct one.
 */
@Getter
public class ExternalExamSitting extends AggregateRoot<String> {

    public enum Status {
        DRAFT, CONFIRMED
    }

    /** A subject of the exam, kept by name too so a renamed or removed school subject doesn't change results. */
    public record ExamSubject(String subjectId, String name) {
    }

    private final String schoolId;
    private final String examId;
    private final String classId;
    private final String academicYearId;
    private String centreNumber;
    private List<ExamSubject> subjects;
    private Status status;
    private String confirmedByName;
    private Instant confirmedAt;

    public ExternalExamSitting(String id, String schoolId, String examId, String classId, String academicYearId,
            String centreNumber, List<ExamSubject> subjects, Status status, String confirmedByName,
            Instant confirmedAt, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.examId = examId;
        this.classId = classId;
        this.academicYearId = academicYearId;
        this.centreNumber = centreNumber;
        this.subjects = subjects == null ? List.of() : List.copyOf(subjects);
        this.status = status == null ? Status.DRAFT : status;
        this.confirmedByName = confirmedByName;
        this.confirmedAt = confirmedAt;
        touch(updatedAt);
    }

    public static ExternalExamSitting create(String id, String schoolId, String examId, String classId,
            String academicYearId, String centreNumber, List<ExamSubject> subjects) {
        Instant now = Instant.now();
        var sitting = new ExternalExamSitting(id, schoolId, examId, classId, academicYearId, null, List.of(),
                Status.DRAFT, null, null, now, now);
        sitting.edit(centreNumber, subjects);
        return sitting;
    }

    public boolean isConfirmed() {
        return status == Status.CONFIRMED;
    }

    public void edit(String centreNumber, List<ExamSubject> subjects) {
        requireDraft();
        if (subjects == null || subjects.isEmpty()) {
            throw new RuleException("Choose the subjects this exam covers");
        }
        if (subjects.stream().map(ExamSubject::subjectId).distinct().count() != subjects.size()) {
            throw new RuleException("A subject is listed twice");
        }
        this.centreNumber = centreNumber == null || centreNumber.isBlank() ? null : centreNumber.trim();
        this.subjects = List.copyOf(subjects);
        touch(Instant.now());
    }

    public void confirm(String byName) {
        requireDraft();
        this.status = Status.CONFIRMED;
        this.confirmedByName = byName;
        this.confirmedAt = Instant.now();
        touch(confirmedAt);
    }

    public void reopen() {
        if (!isConfirmed()) {
            throw new RuleException("These results are not confirmed");
        }
        this.status = Status.DRAFT;
        this.confirmedByName = null;
        this.confirmedAt = null;
        touch(Instant.now());
    }

    public void requireDraft() {
        if (isConfirmed()) {
            throw new RuleException("These results are confirmed. Reopen them to make changes.");
        }
    }
}
