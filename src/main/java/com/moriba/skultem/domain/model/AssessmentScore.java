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

    // The lifecycle only allows APPROVED -> COMPLETED and APPROVED -> LOCKED (see
    // ClassSubjectAssessmentLifeCycle.complete()/lock()) - so a cycle sitting at COMPLETED or
    // LOCKED necessarily passed through APPROVED already. Checking for the literal APPROVED
    // status alone meant every score for a finished (COMPLETED) or archived (LOCKED) term - the
    // overwhelmingly common case once a term is over - was treated as unapproved and hidden,
    // e.g. from SimplifiedClassLeaderBoardUseCase's parent/student-facing subject breakdown.
    public boolean isApproved() {
        return getStatus() == ClassSubjectAssessmentLifeCycle.Status.APPROVED
                || getStatus() == ClassSubjectAssessmentLifeCycle.Status.COMPLETED
                || getStatus() == ClassSubjectAssessmentLifeCycle.Status.LOCKED;
    }

    public boolean isPassed() {
        return score >= getAssessment().getTemplate().getPassMark();
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
