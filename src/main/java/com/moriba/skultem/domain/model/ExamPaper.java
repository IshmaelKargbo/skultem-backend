package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

// One paper on an exam timetable: a subject (optionally "Paper 1", "Paper 2 - Theory", "Practical") sat on a date and
// time, in a venue, by one or more classes together (all of JSS 1 in the hall), watched by one or more invigilators.
@Getter
public class ExamPaper extends AggregateRoot<String> {

    private final String schoolId;
    private final String timetableId;
    private String subjectId;
    private String paper;
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String venue;
    private String notes;
    private final Set<String> classIds = new LinkedHashSet<>();
    private final Set<String> invigilatorIds = new LinkedHashSet<>();

    public ExamPaper(String id, String schoolId, String timetableId, String subjectId, String paper,
            LocalDate examDate, LocalTime startTime, LocalTime endTime, String venue, String notes,
            Collection<String> classIds, Collection<String> invigilatorIds, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.timetableId = timetableId;
        this.subjectId = subjectId;
        this.paper = paper;
        this.examDate = examDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.venue = venue;
        this.notes = notes;
        if (classIds != null) {
            this.classIds.addAll(classIds);
        }
        if (invigilatorIds != null) {
            this.invigilatorIds.addAll(invigilatorIds);
        }
        touch(updatedAt);
    }

    public static ExamPaper create(String schoolId, String timetableId, String subjectId, String paper,
            LocalDate examDate, LocalTime startTime, LocalTime endTime, String venue, String notes,
            Collection<String> classIds, Collection<String> invigilatorIds) {
        Instant now = Instant.now();
        var p = new ExamPaper(UUID.randomUUID().toString(), schoolId, timetableId, null, null, null, null, null,
                null, null, null, null, now, now);
        p.apply(subjectId, paper, examDate, startTime, endTime, venue, notes, classIds, invigilatorIds);
        return p;
    }

    public void update(String subjectId, String paper, LocalDate examDate, LocalTime startTime, LocalTime endTime,
            String venue, String notes, Collection<String> classIds, Collection<String> invigilatorIds) {
        apply(subjectId, paper, examDate, startTime, endTime, venue, notes, classIds, invigilatorIds);
        touch(Instant.now());
    }

    private void apply(String subjectId, String paper, LocalDate examDate, LocalTime startTime, LocalTime endTime,
            String venue, String notes, Collection<String> classIds, Collection<String> invigilatorIds) {
        if (!endTime.isAfter(startTime)) {
            throw new RuleException("The paper has to end after it starts");
        }
        var classes = clean(classIds);
        if (classes.isEmpty()) {
            throw new RuleException("Choose at least one class that sits this paper");
        }
        this.subjectId = subjectId;
        this.paper = blankToNull(paper);
        this.examDate = examDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.venue = blankToNull(venue);
        this.notes = blankToNull(notes);
        this.classIds.clear();
        this.classIds.addAll(classes);
        this.invigilatorIds.clear();
        this.invigilatorIds.addAll(clean(invigilatorIds));
    }

    // Two papers clash when they share a date and their times overlap (one ending as the next starts is fine).
    public boolean overlaps(ExamPaper other) {
        return examDate.equals(other.examDate)
                && startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
    }

    private static Set<String> clean(Collection<String> ids) {
        var out = new LinkedHashSet<String>();
        if (ids != null) {
            ids.stream().filter(i -> i != null && !i.isBlank()).map(String::trim).forEach(out::add);
        }
        return out;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
