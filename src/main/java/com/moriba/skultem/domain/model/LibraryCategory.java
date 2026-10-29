package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class LibraryCategory extends AggregateRoot<String> {
    private String schoolId;
    private String name;
    private String description;

    public LibraryCategory(String id, String schoolId, String name, String description, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.description = description != null ? description : "";
        touch(updatedAt);
    }

    public static LibraryCategory create(String schoolId, String name, String description) {
        Instant now = Instant.now();
        return new LibraryCategory(UUID.randomUUID().toString(), schoolId, name, description, now, now);
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description != null ? description : "";
        touch(Instant.now());
    }
}
