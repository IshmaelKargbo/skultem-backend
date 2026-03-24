package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class AssessmentScore extends AggregateRoot<String> {

    private String schoolId;
    private StudentAssessment studentAssessment;
    private Integer weight;
    private Integer score;
    private Integer weightedScore;
    private ClassSubjectAssessmentLifeCycle cycle;

    public AssessmentScore(
            String id,
            String schoolId,
            StudentAssessment studentAssessment,
            ClassSubjectAssessmentLifeCycle cycle,
            Integer weight,
            Integer score,
            Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);

        this.schoolId = schoolId;
        this.studentAssessment = studentAssessment;
        this.weight = validateWeight(weight);
        this.score = validateScore(score);
        this.cycle = cycle;
        this.weightedScore = calculateWeightedScore(this.score, this.weight);
        touch(updatedAt);
    }

    public static AssessmentScore create(
            String id,
            String schoolId,
            StudentAssessment studentAssessment,
            ClassSubjectAssessmentLifeCycle cycle,
            Integer weight) {
        Instant now = Instant.now();

        return new AssessmentScore(
                id,
                schoolId,
                studentAssessment,
                cycle,
                weight,
                0,
                now,
                now);
    }

    public void updateScore(Integer score) {
        if (!canMark()) {
            throw new RuleException(
                    "You cannot update this score because the assessment is currently "
                            + cycle.getStatus());
        }

        this.score = validateScore(score);
        this.weightedScore = calculateWeightedScore(this.score, this.weight);

        touch(Instant.now());
    }

    private Integer validateScore(Integer value) {
        if (value < 0 || value > 100) {
            throw new RuleException("Score must be between 0 and 100");
        }
        return value;
    }

    private Integer validateWeight(Integer value) {
        if (value <= 0 || value > 100) {
            throw new RuleException("Weight must be between 0 and 100");
        }
        return value;
    }

    private Integer calculateWeightedScore(Integer score, Integer weight) {
        return (int) Math.round((score / 100.0) * weight);
    }

    public boolean canMark() {
        return cycle.canEdit();
    }

    public Assessment getAssessment() {
        return cycle.getAssessment();
    }

    public ClassSubjectAssessmentLifeCycle.Status getStatus() {
        return cycle.getStatus();
    }

    public boolean isDraft() {
        return getStatus() == ClassSubjectAssessmentLifeCycle.Status.DRAFT;
    }

    public boolean isSubmited() {
        return getStatus() == ClassSubjectAssessmentLifeCycle.Status.SUBMITTED;
    }

    public boolean isCompleted() {
        return getStatus() == ClassSubjectAssessmentLifeCycle.Status.COMPLETED;
    }

    public void open() {
        cycle.markDraft();
        touch(Instant.now());
    }

    public void submit() {
        cycle.submit();
        touch(Instant.now());
    }

    public void approve() {
        cycle.approve();
        touch(Instant.now());
    }
}
