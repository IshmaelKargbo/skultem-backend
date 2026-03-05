package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class AssessmentApprovalRequest extends AggregateRoot<String> {

    private String schoolId;
    private ClassSubjectAssessmentLifeCycle cycle;
    private ClassMaster master;
    private TeacherSubject teacherSubject;
    private Term term;
    private String teacherNote;
    private String returnReason;
    private String approvalNote;
    private Status status;

    public enum NoteType {
        TEACHER,
        ADMIN,
        CLASS_MASTER
    }

    public enum Status {
        PENDING_REVIEW,
        RETURNED,
        APPROVED
    }

    public AssessmentApprovalRequest(
            String id,
            String schoolId,
            ClassMaster master,
            ClassSubjectAssessmentLifeCycle cycle,
            TeacherSubject teacherSubject,
            Term term,
            String teacherNote,
            String returnReason,
            String approvalNote,
            Status status,
            Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.master = master;
        this.cycle = cycle;
        this.teacherSubject = teacherSubject;
        this.term = term;
        this.teacherNote = teacherNote;
        this.returnReason = returnReason;
        this.approvalNote = approvalNote;
        this.status = status;

        touch(updatedAt);
    }

    public static AssessmentApprovalRequest create(
            String id,
            String schoolId,
            ClassMaster master,
            ClassSubjectAssessmentLifeCycle cycle,
            TeacherSubject teacherSubject,
            Term term,
            String teacherNote) {
        if (teacherNote == null || teacherNote.isBlank()) {
            throw new RuleException("Submission note is required");
        }

        Instant now = Instant.now();

        return new AssessmentApprovalRequest(id, schoolId, master, cycle, teacherSubject, term,
                teacherNote.trim(), null, null, Status.PENDING_REVIEW, now, now);
    }

    public void returnRequest(String reason) {
        if (status != Status.PENDING_REVIEW) {
            throw new RuleException("Only PENDING_REVIEW requests can be returned");
        }

        if (reason == null || reason.isBlank()) {
            throw new RuleException("Return reason is required");
        }

        status = Status.RETURNED;
        this.returnReason = reason.trim();
        this.approvalNote = null;

        touch(Instant.now());
    }

    public void approve(String note) {
        if (status != Status.PENDING_REVIEW) {
            throw new RuleException("Only PENDING_REVIEW requests can be approved");
        }

        status = Status.APPROVED;
        this.approvalNote = note == null ? null : note.trim();
        touch(Instant.now());
    }

    public void resubmit(String teacherNote) {
        if (status != Status.RETURNED) {
            throw new RuleException("Only RETURNED requests can be resubmitted");
        }

        if (teacherNote == null || teacherNote.isBlank()) {
            throw new RuleException("Resubmission note is required");
        }

        status = Status.PENDING_REVIEW;
        this.teacherNote = teacherNote.trim();

        touch(Instant.now());
    }

    public boolean isPending() {
        return status.equals(Status.PENDING_REVIEW);
    }

    public boolean isApproved() {
        return status.equals(Status.APPROVED);
    }

    public boolean isReturn() {
        return status.equals(Status.RETURNED);
    }
}