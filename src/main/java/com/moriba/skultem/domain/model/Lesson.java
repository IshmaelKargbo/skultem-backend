package com.moriba.skultem.domain.model;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Lesson extends AggregateRoot<String> {

    private final String schoolId;
    private String title;
    private String resource;
    private Week week;
    private LocalDate date;
    private State state;

    public enum State {
        NOT_STARTED, IN_PROGRESS, COMPLETED
    }

    public Lesson(String id, String schoolId, Week week, String resource, LocalDate date, State state,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.week = week;
        this.resource = resource;
        this.date = date;
        this.state = state;
        touch(updatedAt);
    }

    public static Lesson create(String id, String schoolId, Week week, String resource, LocalDate date) {
        Instant now = Instant.now();
        return new Lesson(id, schoolId, week, resource, date, State.NOT_STARTED, now, now);
    }

    public void setState(State state) {
        this.state = state;
        touch(Instant.now());
    }
}
