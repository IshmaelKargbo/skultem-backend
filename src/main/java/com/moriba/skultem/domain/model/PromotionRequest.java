package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.List;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/**
 * A class master's submission to promote every student in one class session at the end of the
 * academic year. Each student is marked PROMOTE (moves to the class's next class) or REPEAT
 * (stays back in the same class), with a remark. Submitting only records the decision - the
 * actual enrollment migration ("the copy process") happens once an admin/proprietor approves.
 */
@Getter
public class PromotionRequest extends AggregateRoot<String> {

    private String schoolId;
    private ClassSession session;
    private ClassMaster master;
    private AcademicYear academicYear;
    private List<PromotionRequestItem> items;
    private String teacherNote;
    private String returnReason;
    private String approvalNote;
    private Status status;
    private Instant executedAt;
    private int promotedCount;
    private int repeatedCount;

    public enum Status {
        PENDING_REVIEW, RETURNED, APPROVED
    }

    public PromotionRequest(String id, String schoolId, ClassSession session, ClassMaster master,
            AcademicYear academicYear, List<PromotionRequestItem> items, String teacherNote, String returnReason,
            String approvalNote, Status status, Instant executedAt, int promotedCount, int repeatedCount,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.session = session;
        this.master = master;
        this.academicYear = academicYear;
        this.items = items;
        this.teacherNote = teacherNote;
        this.returnReason = returnReason;
        this.approvalNote = approvalNote;
        this.status = status;
        this.executedAt = executedAt;
        this.promotedCount = promotedCount;
        this.repeatedCount = repeatedCount;
        touch(updatedAt);
    }

    public static PromotionRequest create(String id, String schoolId, ClassSession session, ClassMaster master,
            AcademicYear academicYear, List<PromotionRequestItem> items, String teacherNote) {
        validateItems(items);

        Instant now = Instant.now();
        return new PromotionRequest(id, schoolId, session, master, academicYear, items,
                teacherNote == null ? null : teacherNote.trim(), null, null, Status.PENDING_REVIEW, null, 0, 0, now,
                now);
    }

    private static void validateItems(List<PromotionRequestItem> items) {
        if (items == null || items.isEmpty()) {
            throw new RuleException("At least one student is required to submit a class for promotion");
        }
    }

    public void resubmit(List<PromotionRequestItem> items, String teacherNote) {
        if (status != Status.RETURNED) {
            throw new RuleException("Only a returned promotion request can be resubmitted");
        }

        validateItems(items);

        this.items = items;
        this.teacherNote = teacherNote == null ? null : teacherNote.trim();
        this.status = Status.PENDING_REVIEW;
        this.returnReason = null;
        touch(Instant.now());
    }

    public void returnRequest(String reason) {
        if (status != Status.PENDING_REVIEW) {
            throw new RuleException("Only a pending promotion request can be returned");
        }

        if (reason == null || reason.isBlank()) {
            throw new RuleException("A reason is required to return a promotion request");
        }

        status = Status.RETURNED;
        returnReason = reason.trim();
        approvalNote = null;
        touch(Instant.now());
    }

    public void approve(String note, int promotedCount, int repeatedCount) {
        if (status != Status.PENDING_REVIEW) {
            throw new RuleException("Only a pending promotion request can be approved");
        }

        status = Status.APPROVED;
        approvalNote = note == null ? null : note.trim();
        executedAt = Instant.now();
        this.promotedCount = promotedCount;
        this.repeatedCount = repeatedCount;
        touch(Instant.now());
    }

    public boolean isPending() {
        return status == Status.PENDING_REVIEW;
    }

    public boolean isApproved() {
        return status == Status.APPROVED;
    }

    public boolean isReturned() {
        return status == Status.RETURNED;
    }
}
