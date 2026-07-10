package com.moriba.skultem.domain.model;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Lesson extends AggregateRoot<String> {

    private final String schoolId;
    private String title;
    private String resource;
    private String assessment;
    private String homework;
    private List<String> objectives;
    private LocalDate date;
    private Week week;
    private State state;

    public enum State {
        PENDING, PUBLISHED, COMPLETED
    }

    public Lesson(String id, String schoolId, Week week, String resource, String assessment, List<String> objectives,
            String homework, LocalDate date, State state, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.week = week;
        this.resource = resource;
        this.assessment = assessment;
        this.objectives = objectives;
        this.homework = homework;
        this.date = date;
        this.state = state;
        touch(updatedAt);
    }

    public static Lesson create(String id, String schoolId, Week week, String resource, String assessment,
            List<String> objectives,
            String homework, LocalDate date) {
        Instant now = Instant.now();
        return new Lesson(id, schoolId, week, resource, assessment, objectives, homework, date, State.PENDING, now,
                now);
    }

    public void setState(State state) {
        this.state = state;
        touch(Instant.now());
    }
}
