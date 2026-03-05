package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class StreamSubject extends AggregateRoot<String> {
    private String schoolId;
    private Stream stream;
    private Subject subject;
    private SubjectGroup group;
    private Boolean mandatory;
    private Boolean locked;

    public StreamSubject(String id, String schoolId, Stream stream, Subject subject, SubjectGroup group,
            Boolean mandatory, Boolean locked, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.stream = stream;
        this.subject = subject;
        this.group = group;
        this.locked = locked;
        this.mandatory = mandatory;
        touch(updatedAt);
    }

    public static StreamSubject create(String id, String schoolId, Stream stream, Subject subject, SubjectGroup group,
            Boolean mandatory) {
        Instant now = Instant.now();
        return new StreamSubject(id, schoolId, stream, subject, group, mandatory, false, now, now);
    }

    public void update(Subject subject, SubjectGroup group, Boolean mandatory) {
        this.group = group;
        this.subject = subject;
        this.mandatory = mandatory;
        touch(Instant.now());
    }

    public void lock() {
        this.locked = true;
    }

    public boolean isLocked() {
        return Boolean.TRUE.equals(this.locked);
    }
}
