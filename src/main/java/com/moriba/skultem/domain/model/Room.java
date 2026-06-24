package com.moriba.skultem.domain.model;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalTime;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Room extends AggregateRoot<String> {

    private final String schoolId;
    private String name;
    private String no;
    private String description;

    public Room(String id, String schoolId, String name, String no, String description, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.no = no;
        this.description = description;
        touch(updatedAt);
    }

    public static Room create(String id, String schoolId, String name, String no, String description) {
        Instant now = Instant.now();
        return new Room(id, schoolId, name, no, description, now, now);
    }

    public void update( String name, String no, String description) {
        this.name = name;
        this.no = no;
        this.description = description;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}