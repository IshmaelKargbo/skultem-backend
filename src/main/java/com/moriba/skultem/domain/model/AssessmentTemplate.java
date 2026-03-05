package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class AssessmentTemplate extends AggregateRoot<String> {

    private String schoolId;
    private String name;
    private String description;

    public AssessmentTemplate(String id, String schoolId, String name, String description, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.schoolId = schoolId;
        this.description = description;
        touch(updatedAt);
    }

    public static AssessmentTemplate create(String id, String schoolId, String name, String description) {
        Instant now = Instant.now();
        return new AssessmentTemplate(id, schoolId, name, description, now, now);
    }
}
