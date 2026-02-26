package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class ClassSection extends AggregateRoot<String> {

    private String schoolId;
    private Clazz clazz;
    private Section section;

    public ClassSection(String id, String schoolId, Clazz clazz, Section section, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.clazz = clazz;
        this.schoolId = schoolId;
        this.section = section;
        touch(updatedAt);
    }

    public static ClassSection create(String id, String schoolId, Clazz clazz, Section section) {
        Instant now = Instant.now();
        return new ClassSection(id, schoolId, clazz, section, now, now);
    }
}
