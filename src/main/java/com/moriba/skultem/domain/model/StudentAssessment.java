package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class StudentAssessment extends AggregateRoot<String> {

    private String schoolId;
    private Enrollment enrollment;
    private TeacherSubject teacherSubject;
    private Term term;

    public StudentAssessment(
            String id,
            String schoolId,
            Enrollment enrollment,
            TeacherSubject teacherSubject,
            Term term,
            Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.enrollment = enrollment;
        this.teacherSubject = teacherSubject;
        this.term = term;
        touch(updatedAt);
    }

    public static StudentAssessment create(
            String id,
            String schoolId,
            Enrollment enrollment,
            TeacherSubject teacherSubject,
            Term term) {
        Instant now = Instant.now();
        return new StudentAssessment(
                id,
                schoolId,
                enrollment,
                teacherSubject,
                term,
                now,
                now
        );
    }
}
