package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.ActivityType;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Activity extends AggregateRoot<String> {

    private final String schoolId;
    private final ActivityType type;
    private final String title;
    private final String subject;
    private final String meta;
    private final String referenceId;

    public Activity(
            String id,
            String schoolId,
            ActivityType type,
            String title,
            String subject,
            String meta,
            String referenceId,
            Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.type = type;
        this.title = title;
        this.subject = subject;
        this.meta = meta;
        this.referenceId = referenceId;
        touch(updatedAt);
    }

    public static Activity create(
            String id,
            String schoolId,
            ActivityType type,
            String title,
            String subject,
            String meta,
            String referenceId) {
        Instant now = Instant.now();
        return new Activity(id, schoolId, type, title, subject, meta, referenceId, now, now);
    }

}
