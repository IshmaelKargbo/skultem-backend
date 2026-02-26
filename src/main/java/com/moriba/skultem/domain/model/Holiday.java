package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Holiday extends AggregateRoot<String> {

    private String schoolId;
    private String name;
    private Kind kind;
    private LocalDate date;
    private AcademicYear academicYear;
    private boolean fixed;

    public enum Kind {
        PUBLIC,
        SCHOOL
    }

    public Holiday(String id, String schoolId, String name, LocalDate date, Kind kind, AcademicYear academicYear,
            boolean fixed,
            Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.schoolId = schoolId;
        this.academicYear = academicYear;
        this.date = date;
        this.fixed = fixed;
        this.kind = kind;
        touch(updatedAt);
    }

    public static Holiday create(String id, String school, String name, LocalDate date, Kind kind,
            AcademicYear academicYear,
            boolean fixed) {
        Instant now = Instant.now();
        return new Holiday(id, school, name, date, kind, academicYear, fixed, now, now);
    }

    public void update(String name, Kind kind, LocalDate date, boolean fixed) {
        this.name = name;
        this.kind = kind;
        this.date = date;
        this.fixed = fixed;
        touch(Instant.now());
    }
}
