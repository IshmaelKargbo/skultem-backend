package com.moriba.skultem.domain.model;

import com.moriba.skultem.application.error.RuleException;

import lombok.Getter;

/**
 * One student's outcome within a {@link PromotionRequest}: whether they move up to the class's
 * next class, or repeat the current one, plus the class master's remark on why.
 *
 * targetStream only applies when promoting into a class that requires a stream (SSS) from one that
 * doesn't (JSS/Primary) - e.g. JSS3 -> SSS1 - since streams are a per-student placement choice made
 * at that boundary, not something carried over from the JSS session.
 */
@Getter
public class PromotionRequestItem {

    private String id;
    private Student student;
    private Enrollment enrollment;
    private Outcome outcome;
    private String remark;
    private Stream targetStream;

    public enum Outcome {
        PROMOTE, REPEAT
    }

    public PromotionRequestItem(String id, Student student, Enrollment enrollment, Outcome outcome, String remark,
            Stream targetStream) {
        this.id = id;
        this.student = student;
        this.enrollment = enrollment;
        this.outcome = outcome;
        this.remark = remark;
        this.targetStream = targetStream;
    }

    public static PromotionRequestItem create(String id, Student student, Enrollment enrollment, Outcome outcome,
            String remark, Stream targetStream) {
        if (outcome == Outcome.REPEAT && (remark == null || remark.isBlank())) {
            throw new RuleException("A remark is required when marking a student to repeat");
        }

        return new PromotionRequestItem(id, student, enrollment, outcome, remark == null ? null : remark.trim(),
                targetStream);
    }

    /**
     * The reviewing admin overriding a REPEAT decision at approval time, without bouncing the whole
     * request back to the class master. The original remark is kept for context.
     */
    public void allowToPass() {
        if (outcome != Outcome.REPEAT) {
            throw new RuleException(student.getName() + " is already marked to promote");
        }

        this.outcome = Outcome.PROMOTE;
    }
}
