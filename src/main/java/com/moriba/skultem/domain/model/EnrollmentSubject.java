package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class EnrollmentSubject extends AggregateRoot<String> {
    private String schoolId;
    private Enrollment enrollment;
    private Subject subject;
    private Student student;

    public EnrollmentSubject(String id, String schoolId, Enrollment enrollement, Subject subject, Student student,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.enrollment = enrollement;
        this.subject = subject;
        this.student = student;
        touch(updatedAt);
    }

    public static EnrollmentSubject create(String id, String schoolId, Enrollment enrollement, Subject subject,
            Student student) {
        Instant now = Instant.now();
        return new EnrollmentSubject(id, schoolId, enrollement, subject, student, now, now);
    }
}
