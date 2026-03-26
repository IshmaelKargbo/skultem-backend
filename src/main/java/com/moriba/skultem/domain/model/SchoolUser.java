package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Role;

import lombok.Getter;

@Getter
public class SchoolUser extends AggregateRoot<String> {

    private String schoolId;
    private User user;
    private Role role;
    private Status status;

    public enum Status {
        ACTIVE,
        INACTIVE
    }

    public SchoolUser(String id, String schoolId, User user, Role role, Status status, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.user = user;
        this.role = role;
        this.status = status;
        touch(updatedAt);
    }

    public static SchoolUser create(String id, String schoolId, User user, Role role) {
        Instant now = Instant.now();
        return new SchoolUser(id, schoolId, user, role, Status.ACTIVE, now, now);
    }

    public void setRole(Role role) {
        this.role = role;
        touch(Instant.now());
    }

    public void setStatus(Status status) {
        this.status = status;
        touch(Instant.now());
    }
}
