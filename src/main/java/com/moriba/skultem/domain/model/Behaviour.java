package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Behaviour extends AggregateRoot<String> {
    private String schoolId;
    private Enrollment enrollment;
    private Kind kind;
    private BehaviourCategory category;

    public enum Kind {
        POSITIVE,
        NEGATIVE,
        NEUTRAL
    }

    public Behaviour(String id, String schoolId, Enrollment enrollment, Kind kind, BehaviourCategory category, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.enrollment = enrollment;
        this.kind = kind;
        this.category = category;
        touch(updatedAt);
    }

    public static Behaviour create(String id, String schoolId, Enrollment enrollment, Kind kind, BehaviourCategory category) {
        Instant now = Instant.now();
        return new Behaviour(id, schoolId, enrollment, kind, category, now, now);
    }
}
