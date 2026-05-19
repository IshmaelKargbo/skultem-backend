package com.moriba.skultem.domain.model;

import java.time.Instant;
import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.Getter;

@Getter
public class House extends AggregateRoot<String> {
    private String schoolId;
    private String name;
    private String motto;
    private String color;
    private Teacher houseMaster;
    private Status status;

    public enum Status {
        ACTIVE, INACTIVE, DELETED
    }

    public House(String id, String schoolId, String name, String motto, String color, Teacher houseMaster,
            Status status, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.motto = motto;
        this.color = color;
        this.houseMaster = houseMaster;
        this.status = status;
        touch(updatedAt);
    }

    public static House create(String id, String schoolId, String name, String motto, String color,
            Teacher houseMaster) {
        Instant now = Instant.now();
        return new House(id, schoolId, name, motto, color, houseMaster, Status.ACTIVE, now, now);
    }

    public void setHouseMaster(Teacher houseMaster) {
        if (houseMaster == null)
            return;
        this.houseMaster = houseMaster;
    }

    public void softDelete() {
        this.status = Status.DELETED;
        touch(Instant.now());
    }
}
