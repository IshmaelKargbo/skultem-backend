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
    private AcademicYear nextYear;
    private Status status;

    public enum Status {
        OPENED,
        CLOSED,
        DELETED
    }

    public AcademicYear(String id, String schoolId, String name, LocalDate startDate, LocalDate endDate, Boolean active, AcademicYear nextYear,
            Status status, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.schoolId = schoolId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.nextYear = nextYear;
        this.active = active;
        this.status = status;
        touch(updatedAt);
    }

    public static AcademicYear create(String id, String schoolId, String name, LocalDate starDate, LocalDate endDate) {
        Instant now = Instant.now();
        return new AcademicYear(id, schoolId, name, starDate, endDate, false, null, Status.OPENED, now, now);
    }

    public void setActive(Boolean state) {
        this.active = state;
        touch(Instant.now());
    }

    public void update(String name, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        touch(Instant.now());
    }

    public void markDeleted() {
        this.status = Status.DELETED;
        touch(Instant.now());
    }

    public void setNext(AcademicYear year) {
        this.nextYear = year;
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
