package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class ClassStream extends AggregateRoot<String> {
    private String schoolId;
    private Stream stream;
    private Clazz clazz;

    public ClassStream(String id, String schoolId, Stream stream, Clazz clazz, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.stream = stream;
        this.clazz = clazz;
        touch(updatedAt);
    }

    public static ClassStream create(String id, String schoolId, Stream stream, Clazz clazz) {
        Instant now = Instant.now();
        return new ClassStream(id, schoolId, stream, clazz, now, now);
    }
}
