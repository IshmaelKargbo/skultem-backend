package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

// The exam timetable for one assessment in one term (e.g. Term 1 "1st Test", Term 1 "Exam"). It has an exam period,
// instructions for candidates and a status: a DRAFT is only seen by the exam office while it is put together; once
// PUBLISHED, teachers and parents see it too. Its papers are ExamPaper rows.
@Getter
public class ExamTimetable extends AggregateRoot<String> {

    public enum Status {
        DRAFT,
        PUBLISHED
    }

    private final String schoolId;
    private final String termId;
    private final String assessmentId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String instructions;
    private Status status;
    private Instant publishedAt;
    private String publishedByName;

    public ExamTimetable(String id, String schoolId, String termId, String assessmentId, String title,
            LocalDate startDate, LocalDate endDate, String instructions, Status status, Instant publishedAt,
            String publishedByName, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.termId = termId;
        this.assessmentId = assessmentId;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.instructions = instructions;
        this.status = status != null ? status : Status.DRAFT;
        this.publishedAt = publishedAt;
        this.publishedByName = publishedByName;
        touch(updatedAt);
    }

    public static ExamTimetable create(String schoolId, String termId, String assessmentId, String title,
            LocalDate startDate, LocalDate endDate, String instructions) {
        Instant now = Instant.now();
        var t = new ExamTimetable(UUID.randomUUID().toString(), schoolId, termId, assessmentId, null, null, null,
                null, Status.DRAFT, null, null, now, now);
        t.applyDetails(title, startDate, endDate, instructions);
        return t;
    }

    public void updateDetails(String title, LocalDate startDate, LocalDate endDate, String instructions) {
        applyDetails(title, startDate, endDate, instructions);
        touch(Instant.now());
    }

    private void applyDetails(String title, LocalDate startDate, LocalDate endDate, String instructions) {
        if (title == null || title.isBlank()) {
            throw new RuleException("Give the timetable a title");
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new RuleException("The exam period has to end on or after its first day");
        }
        this.title = title.trim();
        this.startDate = startDate;
        this.endDate = endDate;
        this.instructions = instructions == null || instructions.isBlank() ? null : instructions.trim();
    }

    public void publish(String byName) {
        this.status = Status.PUBLISHED;
        this.publishedAt = Instant.now();
        this.publishedByName = byName;
        touch(Instant.now());
    }

    // Back to a draft, e.g. to rework it - teachers and parents stop seeing it until it is published again.
    public void unpublish() {
        this.status = Status.DRAFT;
        touch(Instant.now());
    }

    public boolean isPublished() {
        return status == Status.PUBLISHED;
    }

    // Is this date inside the exam period? An open-ended side doesn't limit it.
    public boolean covers(LocalDate date) {
        return (startDate == null || !date.isBefore(startDate)) && (endDate == null || !date.isAfter(endDate));
    }
}
