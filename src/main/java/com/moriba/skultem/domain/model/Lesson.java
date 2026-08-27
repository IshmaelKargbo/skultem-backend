package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.LessonStage;

import lombok.Getter;

/**
 * A single day's lesson note, written by a teacher against a {@link Week} of
 * a scheme of work. Shaped after the standard Sierra Leone lesson note used
 * across schools: topic/sub-topic come from the week, and the note itself
 * captures objectives, previous knowledge, teaching aids, reference
 * materials, the introduction/development/conclusion presentation,
 * evaluation and assignment.
 */
@Getter
public class Lesson extends AggregateRoot<String> {

    private final String schoolId;
    private Week week;
    private String title;
    private String content;
    private LocalDate date;
    private String duration;
    private List<String> objectives;
    private String previousKnowledge;
    private List<String> teachingAids;
    private List<String> referenceMaterials;
    private List<LessonStage> presentation;
    private String evaluation;
    private String assignment;
    private State state;

    public enum State {
        NOT_STARTED, IN_PROGRESS, COMPLETED
    }

    public Lesson(
            String id,
            String schoolId,
            Week week,
            String title,
            String content,
            LocalDate date,
            String duration,
            List<String> objectives,
            String previousKnowledge,
            List<String> teachingAids,
            List<String> referenceMaterials,
            List<LessonStage> presentation,
            String evaluation,
            String assignment,
            State state,
            Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.week = week;
        this.title = validateTitle(title);
        this.content = validateContent(content);
        this.date = validateDate(date);
        this.duration = duration;
        this.objectives = objectives == null ? List.of() : objectives;
        this.previousKnowledge = previousKnowledge;
        this.teachingAids = teachingAids == null ? List.of() : teachingAids;
        this.referenceMaterials = referenceMaterials == null ? List.of() : referenceMaterials;
        this.presentation = presentation == null ? List.of() : presentation;
        this.evaluation = evaluation;
        this.assignment = assignment;
        this.state = state;
        touch(updatedAt);
    }

    public static Lesson create(
            String id,
            String schoolId,
            Week week,
            String title,
            String content,
            LocalDate date,
            String duration,
            List<String> objectives,
            String previousKnowledge,
            List<String> teachingAids,
            List<String> referenceMaterials,
            List<LessonStage> presentation,
            String evaluation,
            String assignment) {
        Instant now = Instant.now();
        return new Lesson(
                id,
                schoolId,
                week,
                title,
                content,
                date,
                duration,
                objectives,
                previousKnowledge,
                teachingAids,
                referenceMaterials,
                presentation,
                evaluation,
                assignment,
                State.NOT_STARTED,
                now,
                now);
    }

    public void update(
            String title,
            String content,
            LocalDate date,
            String duration,
            List<String> objectives,
            String previousKnowledge,
            List<String> teachingAids,
            List<String> referenceMaterials,
            List<LessonStage> presentation,
            String evaluation,
            String assignment) {
        this.title = validateTitle(title);
        this.content = validateContent(content);
        this.date = validateDate(date);
        this.duration = duration;
        this.objectives = objectives == null ? List.of() : objectives;
        this.previousKnowledge = previousKnowledge;
        this.teachingAids = teachingAids == null ? List.of() : teachingAids;
        this.referenceMaterials = referenceMaterials == null ? List.of() : referenceMaterials;
        this.presentation = presentation == null ? List.of() : presentation;
        this.evaluation = evaluation;
        this.assignment = assignment;
        touch(Instant.now());
    }

    public void setState(State state) {
        this.state = state;
        touch(Instant.now());
    }

    private String validateTitle(String value) {
        if (value == null || value.isBlank()) {
            throw new RuleException("Lesson title is required");
        }
        return value;
    }

    private String validateContent(String value) {
        if (value == null || value.isBlank()) {
            throw new RuleException("Lesson content is required");
        }
        return value;
    }

    private LocalDate validateDate(LocalDate value) {
        if (value == null) {
            throw new RuleException("Lesson date is required");
        }
        return value;
    }
}
