package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class ClassMaster extends AggregateRoot<String> {

    private String schoolId;
    private ClassSession session;
    private Teacher teacher;
    private Instant assignAt;
    private Instant endedAt;

    public ClassMaster(String id, String school, ClassSession session, Teacher teacher, Instant assignAt,
            Instant endedAt, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.session = session;
        this.teacher = teacher;
        this.assignAt = assignAt;
        this.endedAt = endedAt;
        this.schoolId = school;
        touch(updatedAt);
    }

    public static ClassMaster create(String id, String school, ClassSession session, Teacher teacher) {
        Instant now = Instant.now();
        return new ClassMaster(id, school, session, teacher, now, null, now, now);
    }

    public boolean isActive() {
        return endedAt == null;
    }

    public void end() {
        this.endedAt = Instant.now();
    }
}
