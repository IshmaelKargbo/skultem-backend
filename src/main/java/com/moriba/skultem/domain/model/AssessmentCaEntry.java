package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

// One continuous-assessment recording ("Week 3") for one student's score in one assessment.
@Getter
public class AssessmentCaEntry extends AggregateRoot<String> {

    private String schoolId;
    private String assessmentScoreId;
    private int entryNumber;
    private int score;
    private String recordedByUserId;

    public AssessmentCaEntry(String id, String schoolId, String assessmentScoreId, int entryNumber, int score,
            String recordedByUserId, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.assessmentScoreId = assessmentScoreId;
        this.entryNumber = entryNumber;
        this.score = validate(score);
        this.recordedByUserId = recordedByUserId;
        touch(updatedAt);
    }

    public static AssessmentCaEntry create(String schoolId, String assessmentScoreId, int entryNumber, int score,
            String userId) {
        Instant now = Instant.now();
        return new AssessmentCaEntry(UUID.randomUUID().toString(), schoolId, assessmentScoreId, entryNumber, score,
                userId, now, now);
    }

    public void update(int score, String userId) {
        this.score = validate(score);
        this.recordedByUserId = userId;
        touch(Instant.now());
    }

    private static int validate(int score) {
        if (score < 0 || score > 100) {
            throw new RuleException("A continuous assessment score must be between 0 and 100");
        }
        return score;
    }
}
