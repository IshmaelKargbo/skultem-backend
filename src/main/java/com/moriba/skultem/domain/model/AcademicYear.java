package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class AcademicYear extends AggregateRoot<String> {

    private String schoolId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private Status status;

    public enum Status {
        OPENED,
        CLOSED,
        DELETED
    }

    public AcademicYear(String id, String schoolId, String name, LocalDate startDate, LocalDate endDate, Boolean active,
            Status status,
            Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.schoolId = schoolId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
        this.status = status;
        touch(updatedAt);
    }

    public static AcademicYear create(String id, String schoolId, String name, LocalDate starDate, LocalDate endDate) {
        Instant now = Instant.now();
        return new AcademicYear(id, schoolId, name, starDate, endDate, false, Status.OPENED, now, now);
    }

    public void setActive(Boolean state) {
        this.active = state;
        touch(Instant.now());
    }

    public boolean isLocked() {
        return this.status == Status.CLOSED;
    }

    public void lock() {
        this.status = Status.CLOSED;
        touch(Instant.now());
    }

    public void open() {
        this.status = Status.OPENED;
        touch(Instant.now());
    }
}
