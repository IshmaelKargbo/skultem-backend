package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class ClassSubject extends AggregateRoot<String> {

    private String schoolId;
    private Clazz clazz;
    private Subject subject;
    private Stream stream;
    private SubjectGroup group;
    private Boolean mandatory;
    private Boolean locked;

    public ClassSubject(String id, String schoolId, Clazz clazz, Subject subject, Stream stream, SubjectGroup group,
            Boolean mandatory, Boolean locked, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.subject = subject;
        this.stream = stream;
        this.group = group;
        this.clazz = clazz;
        this.mandatory = mandatory;
        this.locked = locked == null ? false : locked;
        this.schoolId = schoolId;
        touch(updatedAt);
    }

    public static ClassSubject create(String id, String schoolId, Clazz clazz, Subject subject, Stream stream, SubjectGroup group,
            Boolean mandatory) {
        Instant now = Instant.now();
        return new ClassSubject(id, schoolId, clazz, subject, stream, group, mandatory, false, now, now);
    }

    public void update(Subject subject, Stream stream, SubjectGroup group, Boolean mandatory) {
        this.subject = subject;
        this.stream = stream;
        this.group = group;
        this.mandatory = mandatory;
        touch(Instant.now());
    }

    public void lock() {
        this.locked = true;
    }

    public void unlock() {
        this.locked = false;
    }

    public boolean isLocked() {
        return Boolean.TRUE.equals(this.locked);
    }
}
