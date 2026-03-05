package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class TeacherSubject extends AggregateRoot<String> {

    private String schoolId;
    private ClassSession session;
    private Teacher teacher;
    private Subject subject;
    private Instant assignedAt;

    public TeacherSubject(String id, String school, ClassSession session, Teacher teacher, Subject subject,
            Instant assignedAt, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.session = session;
        this.teacher = teacher;
        this.assignedAt = assignedAt;
        this.subject = subject;
        this.schoolId = school;
        touch(updatedAt);
    }

    public static TeacherSubject create(String id, String school, ClassSession session, Teacher teacher,
            Subject subject) {
        Instant now = Instant.now();
        return new TeacherSubject(id, school, session, teacher, subject, now, now, now);
    }

    public void changeTeacher(Teacher teacher) {
        this.teacher = teacher;
        touch(Instant.now());
    }
}
