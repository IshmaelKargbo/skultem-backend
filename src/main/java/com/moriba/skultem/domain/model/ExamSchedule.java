package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

// One paper on the exam timetable: a class sits a subject (optionally "Paper 1", "Paper 2") on a date and time,
// in a venue, watched by an invigilator. Kept by the exam office (see ExamScheduleController).
@Getter
public class ExamSchedule extends AggregateRoot<String> {

    private final String schoolId;
    private String termId;
    private String classId;
    private String subjectId;
    private String paper;
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String venue;
    private String invigilatorId;
    private String notes;

    public ExamSchedule(String id, String schoolId, String termId, String classId, String subjectId, String paper,
            LocalDate examDate, LocalTime startTime, LocalTime endTime, String venue, String invigilatorId,
            String notes, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.termId = termId;
        this.classId = classId;
        this.subjectId = subjectId;
        this.paper = paper;
        this.examDate = examDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.venue = venue;
        this.invigilatorId = invigilatorId;
        this.notes = notes;
        touch(updatedAt);
    }

    public static ExamSchedule create(String id, String schoolId, String termId, String classId, String subjectId,
            String paper, LocalDate examDate, LocalTime startTime, LocalTime endTime, String venue,
            String invigilatorId, String notes) {
        Instant now = Instant.now();
        return new ExamSchedule(id, schoolId, termId, classId, subjectId, blankToNull(paper), examDate, startTime,
                endTime, blankToNull(venue), blankToNull(invigilatorId), blankToNull(notes), now, now);
    }

    public void update(String termId, String classId, String subjectId, String paper, LocalDate examDate,
            LocalTime startTime, LocalTime endTime, String venue, String invigilatorId, String notes) {
        this.termId = termId;
        this.classId = classId;
        this.subjectId = subjectId;
        this.paper = blankToNull(paper);
        this.examDate = examDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.venue = blankToNull(venue);
        this.invigilatorId = blankToNull(invigilatorId);
        this.notes = blankToNull(notes);
        touch(Instant.now());
    }

    // Two papers clash when they share a date and their times overlap (one ending as the next starts is fine).
    public boolean overlaps(ExamSchedule other) {
        return examDate.equals(other.examDate)
                && startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
