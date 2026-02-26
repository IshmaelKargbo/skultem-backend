package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Stream extends AggregateRoot<String> {

    private String schoolId;
    private String name;
    private String description;

    public Stream(String id, String name, String schoolId, String description, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.schoolId = schoolId;
        this.description = description;
        touch(updatedAt);
    }

    public static Stream create(String id, String name, String schoolId, String description) {
        Instant now = Instant.now();
        return new Stream(id, name, schoolId, description, now, now);
    }
}
