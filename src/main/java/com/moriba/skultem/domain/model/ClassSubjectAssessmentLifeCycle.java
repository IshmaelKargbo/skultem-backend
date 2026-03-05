package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class ClassSubjectAssessmentLifeCycle extends AggregateRoot<String> {

    private String schoolId;
    private Term term;
    private Assessment assessment;
    private TeacherSubject subject;
    private Status status;

    public enum Status {
        DRAFT,
        SUBMITTED,
        RETURNED,
        APPROVED,
        COMPLETED,
        LOCKED
    }

    public ClassSubjectAssessmentLifeCycle(String id, String schoolId, TeacherSubject subject, Term term,
            Assessment assessment, Status status, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.term = term;
        this.schoolId = schoolId;
        this.assessment = assessment;
        this.subject = subject;
        this.status = status;
        touch(updatedAt);
    }

    public static ClassSubjectAssessmentLifeCycle create(String id, String schoolId, TeacherSubject subject, Term term,
            Assessment assessment, Status status) {
        Instant now = Instant.now();
        return new ClassSubjectAssessmentLifeCycle(id, schoolId, subject, term, assessment, status, now, now);
    }

    public boolean isDraft() {
        return status.equals(Status.DRAFT);
    }
    
public boolean canEdit() {
    return status == Status.DRAFT || status == Status.RETURNED;
}

    public void markDraft() {
        status = Status.DRAFT;
        touch(Instant.now());
    }

    public void submit() {
        if (status != Status.DRAFT && status != Status.RETURNED) {
            throw new RuleException("Only DRAFT or RETURNED assessments can be submitted");
        }
        status = Status.SUBMITTED;
        touch(Instant.now());
    }

    public void approve() {
        if (status != Status.SUBMITTED) {
            throw new RuleException("Only SUBMITTED assessments can be approved");
        }
        status = Status.APPROVED;
        touch(Instant.now());
    }

    public void returnForCorrection() {
        if (status != Status.SUBMITTED) {
            throw new RuleException("Only SUBMITTED assessments can be returned");
        }
        status = Status.RETURNED;
        touch(Instant.now());
    }

    public void lock() {
        if (status != Status.APPROVED) {
            throw new RuleException("Only APPROVED assessments can be locked");
        }
        status = Status.LOCKED;
        touch(Instant.now());
    }

    public void complete() {
        if (status != Status.APPROVED) {
            throw new RuleException("Only APPROVED assessments can be completed");
        }
        status = Status.COMPLETED;
        touch(Instant.now());
    }
}
