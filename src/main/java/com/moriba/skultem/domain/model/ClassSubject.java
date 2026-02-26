package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class ClassSubject extends AggregateRoot<String> {

    private String schoolId;
    private Clazz clazz;
    private Subject subject;
    private SubjectGroup group;
    private Boolean mandatory;

    public ClassSubject(String id, String schoolId, Clazz clazz, Subject subject, SubjectGroup group,
            Boolean mandatory, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.subject = subject;
        this.group = group;
        this.clazz = clazz;
        this.mandatory = mandatory;
        this.schoolId = schoolId;
        touch(updatedAt);
    }

    public static ClassSubject create(String id, String schoolId, Clazz clazz, Subject subject, SubjectGroup group,
            Boolean mandatory) {
        Instant now = Instant.now();
        return new ClassSubject(id, schoolId, clazz, subject, group, mandatory, now, now);
    }

    public void update(Subject subject, SubjectGroup group, Boolean mandatory) {
        this.subject = subject;
        this.group = group;
        this.mandatory = mandatory;
        touch(Instant.now());
    }
}
