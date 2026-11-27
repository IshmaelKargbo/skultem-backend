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
    // The management section this is for; null = the whole school.
    private String managementSectionId;
    // The school is closed on these days: they come off the school days attendance is counted against.
    // Only a whole-school entry can close the school.
    private boolean noSchool;

    public enum Type {
        EVENT,
        HOLIDAY
    }

    public CalendarEvent(String id, String schoolId, String title, String description, Type type, Instant startDate,
            Instant endDate, String location, String createdByUserId, String createdByName,
            String managementSectionId, boolean noSchool, Instant createdAt, Instant updatedAt) {
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
        this.managementSectionId = managementSectionId;
        this.noSchool = noSchool && managementSectionId == null;
        touch(updatedAt);
    }

    public static CalendarEvent create(String id, String schoolId, String title, String description, Type type,
            Instant startDate, Instant endDate, String location, String createdByUserId, String createdByName,
            String managementSectionId, boolean noSchool) {
        Instant now = Instant.now();
        return new CalendarEvent(id, schoolId, title, description, type, startDate, endDate, location,
                createdByUserId, createdByName, managementSectionId, noSchool, now, now);
    }

    public void update(String title, String description, Type type, Instant startDate, Instant endDate,
            String location, boolean noSchool) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.noSchool = noSchool && managementSectionId == null;
        touch(Instant.now());
    }
}
