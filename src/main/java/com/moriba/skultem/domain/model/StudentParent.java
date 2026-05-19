package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class StudentParent extends AggregateRoot<String> {
    private String schoolId;
    private String relationship;
    private Parent parent;
    private Student student;
    private Status status;

    public enum Status {
        ACTIVE, DELETED
    }

    public StudentParent(String id, String schoolId, String relationship, Parent parent, Student student, Status status,
            Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.relationship = relationship;
        this.parent = parent;
        this.student = student;
        this.status = status;
        touch(updatedAt);
    }

    public static StudentParent create(String schoolId, String relationship, Parent parent, Student student) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new StudentParent(id, schoolId, relationship, parent, student, Status.ACTIVE, now, now);
    }

    public void softDelete() {
        this.status = Status.DELETED;
        touch(Instant.now());
    }
}
