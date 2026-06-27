package com.moriba.skultem.domain.model;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = true)
public class LessionPlan extends AggregateRoot<String> {

    private final String schoolId;
    private final String title;
    private LocalDate date;
    private String materials;
    private String resources;
    private String assessment;
    private List<String> objectives;
    private List<String> activities;
    private final Week week;
    private String homework;
    private State state;

    public enum State {
        NOT_STARTED, IN_PROGRESS, COMPLETED
    }

    public LessionPlan(String id, String schoolId, String title, LocalDate date, String materials, String homework,
            String resources, List<String> objectives, List<String> activities, Week week, State state,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.title = title;
        this.date = date;
        this.homework = homework;
        this.materials = materials;
        this.resources = resources;
        this.objectives = objectives;
        this.week = week;
        this.activities = activities;
        this.state = state;
        touch(updatedAt);
    }

    public static LessionPlan create(String id, String schoolId, String title, LocalDate date, String materials,
            String homework, String resources, List<String> objectives, List<String> activities, Week week) {
        Instant now = Instant.now();
        return new LessionPlan(id, schoolId, title, date, materials, homework, resources, objectives, activities, week,
                State.NOT_STARTED, now, now);
    }

    public void setState(State state) {
        this.state = state;
        touch(Instant.now());
    }
}
