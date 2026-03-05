package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Assessment extends AggregateRoot<String> {

    private String schoolId;
    private AssessmentTemplate template;
    private String name;
    private Integer weight;
    private int position;

    public Assessment(String id, String schoolId, AssessmentTemplate template, String name, Integer weight, int position, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.schoolId = schoolId;
        this.template = template;
        this.weight = validateWeight(weight);
        this.position = position;
        touch(updatedAt);
    }

    public static Assessment create(String id, String schoolId, AssessmentTemplate template, String name, Integer weight, int position) {
        Instant now = Instant.now();
        return new Assessment(id, schoolId, template, name, weight, position, now, now);
    }

    public void update(String name, Integer weight, int position) {
        this.name = name;
        this.weight = validateWeight(weight);
        this.position = position;
        touch(Instant.now());
    }

    private static Integer validateWeight(Integer weight) {
        if (weight == null || weight <= 0 || weight > 100) {
            throw new IllegalArgumentException("Assessment weight must be greater than 0 and not exceed 100");
        }
        return weight;
    }
}
