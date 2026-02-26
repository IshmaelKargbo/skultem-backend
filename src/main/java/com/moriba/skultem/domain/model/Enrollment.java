package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Enrollment extends AggregateRoot<String> {
    private String schoolId;
    private Student student;
    private Clazz clazz;
    private AcademicYear academicYear;
    private Section section;
    private Stream stream;
    private Status status;

    public enum Status {
        ACTIVE, PROMOTED, REPEATED, LEFT
    }

    public Enrollment(String id, String schoolId, Student student, Clazz clazz, Section section,
            AcademicYear academicYear, Stream stream, Status status, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.student = student;
        this.clazz = clazz;
        this.section = section;
        this.stream = stream;
        this.academicYear = academicYear;
        this.status = status;
        touch(updatedAt);
    }

    public static Enrollment create(String id, String schoolId, Student student, Clazz clazz, Section section,
            AcademicYear academicYear, Stream stream) {
        Instant now = Instant.now();
        return new Enrollment(id, schoolId, student, clazz, section, academicYear, stream, Status.ACTIVE, now, now);
    }
}
