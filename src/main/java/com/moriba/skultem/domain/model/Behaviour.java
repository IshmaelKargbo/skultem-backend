package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.model.vo.Kind;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Behaviour extends AggregateRoot<String> {
    private String schoolId;
    private Enrollment enrollment;
    private Kind kind;
    private BehaviourCategory category;
    private String note;

    public Behaviour(String id, String schoolId, Enrollment enrollment, Kind kind, BehaviourCategory category, String note, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.enrollment = enrollment;
        this.kind = kind;
        this.category = category;
        this.note = note;
        touch(updatedAt);
    }

    public static Behaviour create(String id, String schoolId, Enrollment enrollment, Kind kind, BehaviourCategory category, String note) {
        Instant now = Instant.now();
        return new Behaviour(id, schoolId, enrollment, kind, category, note, now, now);
    }
}
