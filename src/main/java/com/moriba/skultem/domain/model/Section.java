package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Section extends AggregateRoot<String> {

    private String schoolId;
    private String name;
    private String description;

    public Section(String id, String schoolId, String name, String description, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.description = description;
        this.schoolId = schoolId;
        touch(updatedAt);
    }

    public static Section create(String id, String schoolId, String name, String description) {
        Instant now = Instant.now();
        return new Section(id, schoolId, name, description, now, now);
    }
}
