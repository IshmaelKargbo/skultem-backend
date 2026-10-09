package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Section extends AggregateRoot<String> {

    private String schoolId;
    private String name;
    private String description;
    // Rank among the school's sections (A before B before C), lowest first. Class lists follow it.
    private int displayOrder;

    public Section(String id, String schoolId, String name, String description, int displayOrder,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.description = description;
        this.schoolId = schoolId;
        this.displayOrder = displayOrder;
        touch(updatedAt);
    }

    public static Section create(String id, String schoolId, String name, String description) {
        return create(id, schoolId, name, description, 0);
    }

    public static Section create(String id, String schoolId, String name, String description, int displayOrder) {
        Instant now = Instant.now();
        return new Section(id, schoolId, name, description, displayOrder, now, now);
    }

    public void moveTo(int displayOrder) {
        this.displayOrder = displayOrder;
        touch(Instant.now());
    }
}
