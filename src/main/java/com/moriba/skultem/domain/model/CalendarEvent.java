package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class CalendarEvent extends AggregateRoot<String> {

    private String schoolId;
    private String title;
    private String description;
    private Type type;
    private Instant startDate;
    private Instant endDate;
    private String location;
    private String createdByUserId;
    private String createdByName;

    public enum Type {
        EVENT,
        HOLIDAY
    }

    public CalendarEvent(String id, String schoolId, String title, String description, Type type, Instant startDate,
            Instant endDate, String location, String createdByUserId, String createdByName,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.createdByUserId = createdByUserId;
        this.createdByName = createdByName;
        touch(updatedAt);
    }

    public static CalendarEvent create(String id, String schoolId, String title, String description, Type type,
            Instant startDate, Instant endDate, String location, String createdByUserId, String createdByName) {
        Instant now = Instant.now();
        return new CalendarEvent(id, schoolId, title, description, type, startDate, endDate, location,
                createdByUserId, createdByName, now, now);
    }

    public void update(String title, String description, Type type, Instant startDate, Instant endDate,
            String location) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        touch(Instant.now());
    }
}
