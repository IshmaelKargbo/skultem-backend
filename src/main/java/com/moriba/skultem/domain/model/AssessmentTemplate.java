package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class AssessmentTemplate extends AggregateRoot<String> {

    private String schoolId;
    private String name;
    private String description;
    private int passMark;

    public AssessmentTemplate(String id, String schoolId, String name, int passMark, String description, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.schoolId = schoolId;
        this.passMark = passMark;
        this.description = description;
        touch(updatedAt);
    }

    public static AssessmentTemplate create(String id, String schoolId, String name, String description, int passMark) {
        Instant now = Instant.now();
        return new AssessmentTemplate(id, schoolId, name, passMark, description, now, now);
    }
}
