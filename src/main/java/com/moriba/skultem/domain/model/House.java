package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.Getter;

@Getter
public class House extends AggregateRoot<String> {

    private String schoolId;
    private String name;
    private String motto;
    private String color;
    private List<Teacher> houseMasters;
    private Status status;

    public enum Status {
        ACTIVE,
        INACTIVE,
        DELETED
    }

    public House(
            String id,
            String schoolId,
            String name,
            String motto,
            String color,
            List<Teacher> houseMasters,
            Status status,
            Instant createdAt,
            Instant updatedAt) {

        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.motto = motto;
        this.color = color;
        this.houseMasters = houseMasters != null
                ? new ArrayList<>(houseMasters)
                : new ArrayList<>();
        this.status = status;

        touch(updatedAt);
    }

    public static House create(
            String id,
            String schoolId,
            String name,
            String motto,
            String color,
            List<Teacher> houseMasters) {

        Instant now = Instant.now();

        return new House(
                id,
                schoolId,
                name,
                motto,
                color,
                houseMasters,
                Status.ACTIVE,
                now,
                now);
    }

    public void addMaster(Teacher houseMaster) {
        if (houseMaster == null) {
            return;
        }

        boolean exists = houseMasters.stream()
                .anyMatch(master -> master.getId().equals(houseMaster.getId()));

        if (!exists) {
            houseMasters.add(houseMaster);
            touch(Instant.now());
        }
    }

    public void removeMaster(String teacherId) {
        if (teacherId == null) {
            return;
        }

        houseMasters.removeIf(master -> master.getId().equals(teacherId));
        touch(Instant.now());
    }

    public void update(
            String name,
            String motto,
            String color) {

        this.name = name;
        this.motto = motto;
        this.color = color;

        touch(Instant.now());
    }

    public void activate() {
        this.status = Status.ACTIVE;
        touch(Instant.now());
    }

    public void deactivate() {
        this.status = Status.INACTIVE;
        touch(Instant.now());
    }

    public void softDelete() {
        this.status = Status.DELETED;
        touch(Instant.now());
    }
}