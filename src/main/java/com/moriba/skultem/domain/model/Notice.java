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
    // When the thing this notice announces takes place (a PTA meeting), where, and the calendar entry
    // mirroring it - all optional; a plain notice has none.
    private Instant eventAt;
    private Instant eventEndsAt;
    private String eventLocation;
    private String calendarEventId;
    // The management section this is for; null = the whole school.
    private String managementSectionId;

    public enum Category {
        GENERAL,
        ACADEMIC,
        FEE,
        URGENT,
        // About something that happens on a date - a PTA meeting, a sports day (see eventAt).
        EVENT
    }

    public Notice(String id, String schoolId, String title, String content, Category category, Audience audience,
            boolean pinned, String postedByUserId, String postedByName, Instant expiresAt, Instant eventAt,
            Instant eventEndsAt, String eventLocation, String calendarEventId, String managementSectionId,
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
        this.eventAt = eventAt;
        this.eventEndsAt = eventEndsAt;
        this.eventLocation = eventLocation;
        this.calendarEventId = calendarEventId;
        this.managementSectionId = managementSectionId;
        touch(updatedAt);
    }

    public static Notice create(String id, String schoolId, String title, String content, Category category,
            Audience audience, Instant expiresAt, Instant eventAt, Instant eventEndsAt, String eventLocation,
            String managementSectionId, String postedByUserId, String postedByName) {
        Instant now = Instant.now();
        return new Notice(id, schoolId, title, content, category, audience, false, postedByUserId, postedByName,
                expiresAt, eventAt, eventEndsAt, eventLocation, null, managementSectionId, now, now);
    }

    public void update(String title, String content, Category category, Audience audience, Instant expiresAt,
            Instant eventAt, Instant eventEndsAt, String eventLocation) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.audience = audience;
        this.expiresAt = expiresAt;
        this.eventAt = eventAt;
        this.eventEndsAt = eventEndsAt;
        this.eventLocation = eventLocation;
        touch(Instant.now());
    }

    public void linkCalendarEvent(String calendarEventId) {
        this.calendarEventId = calendarEventId;
        touch(Instant.now());
    }

    public void togglePin() {
        this.pinned = !this.pinned;
        touch(Instant.now());
    }
}
