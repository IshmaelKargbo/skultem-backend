package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ReportConfig extends AggregateRoot<String> {
    private final String schoolId;
    private String name;
    private String type;
    private String format;
    private String classId;
    private String classSessionId;
    private String teacherSubjectId;
    private String termId;
    private LocalDate startDate;
    private LocalDate endDate;

    public ReportConfig(
            String id,
            String schoolId,
            String name,
            String type,
            String format,
            String classId,
            String classSessionId,
            String teacherSubjectId,
            String termId,
            LocalDate startDate,
            LocalDate endDate,
            Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.type = type;
        this.format = format;
        this.classId = classId;
        this.classSessionId = classSessionId;
        this.teacherSubjectId = teacherSubjectId;
        this.termId = termId;
        this.startDate = startDate;
        this.endDate = endDate;
        touch(updatedAt);
    }

    public static ReportConfig create(
            String id,
            String schoolId,
            String name,
            String type,
            String format,
            String classId,
            String classSessionId,
            String teacherSubjectId,
            String termId,
            LocalDate startDate,
            LocalDate endDate) {
        Instant now = Instant.now();
        return new ReportConfig(
                id,
                schoolId,
                name,
                type,
                format,
                classId,
                classSessionId,
                teacherSubjectId,
                termId,
                startDate,
                endDate,
                now,
                now);
    }
}
