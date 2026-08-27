package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class PromotionConfig extends AggregateRoot<String> {

    private String schoolId;
    private Integer minPassMark;
    private int maxRepeatCount;
    private boolean requireApproval;
    private boolean requireRemarkForPromote;

    public PromotionConfig(String id, String schoolId, Integer minPassMark, int maxRepeatCount,
            boolean requireApproval, boolean requireRemarkForPromote, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.minPassMark = minPassMark;
        this.maxRepeatCount = maxRepeatCount;
        this.requireApproval = requireApproval;
        this.requireRemarkForPromote = requireRemarkForPromote;
        touch(updatedAt);
    }

    public static PromotionConfig createDefault(String id, String schoolId) {
        Instant now = Instant.now();
        return new PromotionConfig(id, schoolId, null, 2, true, false, now, now);
    }

    public void update(Integer minPassMark, int maxRepeatCount, boolean requireApproval,
            boolean requireRemarkForPromote) {
        this.minPassMark = minPassMark;
        this.maxRepeatCount = maxRepeatCount;
        this.requireApproval = requireApproval;
        this.requireRemarkForPromote = requireRemarkForPromote;
        touch(Instant.now());
    }
}
