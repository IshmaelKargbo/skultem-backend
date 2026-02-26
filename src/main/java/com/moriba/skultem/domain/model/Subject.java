package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Subject extends AggregateRoot<String> {

    private String schoolId;
    private String name;
    private String code;
    private String description;

    public Subject(String id, String schoolId, String name, String code, String description, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.description = description;
        this.name = name;
        this.schoolId = schoolId;
        this.code = code;
        touch(updatedAt);
    }

    public static Subject create(String id, String schoolId, String name, String code, String description) {
        Instant now = Instant.now();
        return new Subject(id, schoolId, name, code, description, now, now);
    }
}
