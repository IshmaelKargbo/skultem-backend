package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.Map;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Priority;

import lombok.Getter;

@Getter
public class Notification extends AggregateRoot<String> {

    private String schoolId;
    private User owner;
    private Type type;
    private String title;
    private Priority priority;
    private String message;
    private Map<String, String> meta;
    private boolean read;

    public enum Type {
        ATTENDANCE,
        ANNOUNCEMENT,
        REMINDER,
        BEHAVIOUR,
        FEE,
        ASSESSMENT
    }

    public Notification(String id, String schoolId, User owner, Type type,
            String title, String message, Map<String, String> meta, Priority priority,
            boolean read, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.owner = owner;
        this.type = type;
        this.title = title;
        this.priority = priority;
        this.message = message;
        this.meta = meta;
        this.read = read;
        touch(updatedAt);
    }

    // Factory method
    public static Notification create(String id, String schoolId, User owner, Type type,
            String title, String message, Map<String, String> meta, Priority priority) {
        Instant now = Instant.now();
        return new Notification(id, schoolId, owner, type, title, message, meta, priority, false, now, now);
    }

    // Update method
    public void update(Type type, String title, String message, Map<String, String> meta, boolean read) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.meta = meta;
        this.read = read;
        touch(Instant.now());
    }

    // Mark as read
    public void markAsRead() {
        this.read = true;
        touch(Instant.now());
    }
}