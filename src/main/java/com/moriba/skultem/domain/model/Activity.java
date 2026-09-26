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
    // The management section it happened in - set when a section-limited admin did it; null = the school as a whole.
    private final String managementSectionId;

    public Activity(String id, String schoolId, ActivityType type, String title, String subject, String meta,
            String referenceId, Instant createdAt, Instant updatedAt) {
        this(id, schoolId, null, type, title, subject, meta, referenceId, createdAt, updatedAt);
    }

    public Activity(String id, String schoolId, String managementSectionId, ActivityType type, String title,
            String subject, String meta, String referenceId, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.managementSectionId = managementSectionId;
        this.schoolId = schoolId;
        this.type = type;
        this.title = title;
        this.subject = subject;
        this.meta = meta;
        this.referenceId = referenceId;
        touch(updatedAt);
    }

    public static Activity createInSection(String id, String schoolId, String managementSectionId, ActivityType type,
            String title, String subject, String meta, String referenceId) {
        Instant now = Instant.now();
        return new Activity(id, schoolId, managementSectionId, type, title, subject, meta, referenceId, now, now);
    }

    public static Activity create(String id, String schoolId, ActivityType type,
            String title, String subject, String meta, String referenceId) {
        Instant now = Instant.now();
        return new Activity(id, schoolId, type, title, subject, meta, referenceId, now, now);
    }

}
