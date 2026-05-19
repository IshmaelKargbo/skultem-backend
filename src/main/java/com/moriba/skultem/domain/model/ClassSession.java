package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class ClassSession extends AggregateRoot<String> {

    private String schoolId;
    private Clazz clazz;
    private Stream stream;
    private Section section;
    private AcademicYear academicYear;

    public ClassSession(String id, String schoolId, Clazz clazz, Stream stream, Section section,
            AcademicYear academicYear,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.clazz = clazz;
        this.stream = stream;
        this.section = section;
        this.academicYear = academicYear;
        this.schoolId = schoolId;
        touch(updatedAt);
    }

    public String getName() {
        StringBuilder builder = new StringBuilder();

        if (clazz.getName() != null && !clazz.getName().isBlank()) {
            builder.append(clazz.getName());
        }

        if (section != null && !section.getName().isBlank()) {
            builder.append(" ").append(section.getName());
        }

        if (stream != null && !stream.getName().isBlank()) {
            builder.append(" ").append(stream.getName());
        }

        return builder.toString().trim();
    }

    public static ClassSession create(String id, String schoolId, Clazz clazz, Stream stream, Section section,
            AcademicYear academicYear) {
        Instant now = Instant.now();
        return new ClassSession(id, schoolId, clazz, stream, section, academicYear, now, now);
    }

}
