package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.Getter;

@Getter
public class MaterialCategory extends AggregateRoot<String> {
    private String schoolId;
    private String name;
    private String description;

    public MaterialCategory(String id, String schoolId, String name, String description, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.description = description;
        touch(updatedAt);
    }

    public static MaterialCategory create(String schoolId, String name, String description) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new MaterialCategory(id, schoolId, name, description, now, now);
    }
}
