package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Term extends AggregateRoot<String> {

    private String schoolId;
    private String name;
    private int termNumber;
    private AcademicYear academicYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private Status status;

    public enum Status {
        UPCOMING,
        ACTIVE,
        CLOSED
    }

    public Term(String id, String schoolId, String name, int termNumber, LocalDate startDate, LocalDate endDate,
            AcademicYear academicYear, Status status, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.termNumber = termNumber;
        this.academicYear = academicYear;
        this.schoolId = schoolId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        touch(updatedAt);
    }

    public static Term create(String id, String schoolId, AcademicYear academicYear, String name, int termNumber, Status status,
            LocalDate starDate, LocalDate endDate) {
        Instant now = Instant.now();
        return new Term(id, schoolId, name, termNumber, starDate, endDate, academicYear, status, now,
                now);
    }


    public boolean isLocked() {
        return this.status == Status.CLOSED;
    }

    public void lock() {
        this.status = Status.CLOSED;
        touch(Instant.now());
    }
}
