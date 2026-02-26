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

    public StreamSubject(String id, String schoolId, Stream stream, Subject subject, SubjectGroup group,
            Boolean mandatory, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.stream = stream;
        this.subject = subject;
        this.group = group;
        this.mandatory = mandatory;
        touch(updatedAt);
    }

    public static StreamSubject create(String id, String schoolId, Stream stream, Subject subject, SubjectGroup group,
            Boolean mandatory) {
        Instant now = Instant.now();
        return new StreamSubject(id, schoolId, stream, subject, group, mandatory, now, now);
    }

    public void update(SubjectGroup group, Boolean mandatory) {
        this.group = group;
        this.mandatory = mandatory;
        touch(Instant.now());
    }
}
