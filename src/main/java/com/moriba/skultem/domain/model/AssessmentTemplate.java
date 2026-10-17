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
    // The term (1, 2, 3...) students of classes using this template start being graded in. A class that joins part-way
    // through the year (SSS 1 in the second term) isn't graded or billed for the terms before.
    private int startsInTerm = 1;

    public AssessmentTemplate(String id, String schoolId, String name, int passMark, String description, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.schoolId = schoolId;
        this.passMark = passMark;
        this.description = description;
        touch(updatedAt);
    }

    public void updateDetails(String name, int passMark, String description) {
        this.name = name;
        this.passMark = passMark;
        this.description = description;
        touch(Instant.now());
    }

    public AssessmentTemplate startsInTerm(int termNumber) {
        this.startsInTerm = Math.max(1, termNumber);
        return this;
    }

    public void changeStartsInTerm(int termNumber) {
        startsInTerm(termNumber);
        touch(Instant.now());
    }

    public static AssessmentTemplate create(String id, String schoolId, String name, String description, int passMark) {
        Instant now = Instant.now();
        return new AssessmentTemplate(id, schoolId, name, passMark, description, now, now);
    }
}
