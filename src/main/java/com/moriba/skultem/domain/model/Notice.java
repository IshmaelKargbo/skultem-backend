package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Audience;

import lombok.Getter;

@Getter
public class Notice extends AggregateRoot<String> {

    private String schoolId;
    private String title;
    private String content;
    private Category category;
    private Audience audience;
    private boolean pinned;
    private String postedByUserId;
    private String postedByName;
    private Instant expiresAt;

    public enum Category {
        GENERAL,
        ACADEMIC,
        FEE,
        URGENT
    }

    public Notice(String id, String schoolId, String title, String content, Category category, Audience audience,
            boolean pinned, String postedByUserId, String postedByName, Instant expiresAt,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.audience = audience;
        this.pinned = pinned;
        this.postedByUserId = postedByUserId;
        this.postedByName = postedByName;
        this.expiresAt = expiresAt;
        touch(updatedAt);
    }

    public static Notice create(String id, String schoolId, String title, String content, Category category,
            Audience audience, Instant expiresAt, String postedByUserId, String postedByName) {
        Instant now = Instant.now();
        return new Notice(id, schoolId, title, content, category, audience, false, postedByUserId, postedByName,
                expiresAt, now, now);
    }

    public void update(String title, String content, Category category, Audience audience, Instant expiresAt) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.audience = audience;
        this.expiresAt = expiresAt;
        touch(Instant.now());
    }

    public void togglePin() {
        this.pinned = !this.pinned;
        touch(Instant.now());
    }
}
