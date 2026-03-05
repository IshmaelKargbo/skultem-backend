package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class User extends AggregateRoot<String> {

    private String email;
    private String givenNames;
    private String familyName;
    private String password;
    private String hint;
    private Status status;

    public enum Status {
        ACTIVE,
        INACTIVE,
        DELETED
    }

    public User(String id, String givenNames, String familyName, String email, String password, String hint,
            Status status, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.givenNames = givenNames;
        this.familyName = familyName;
        this.email = email;
        this.password = password;
        this.hint = hint;
        this.status = status;
        touch(updatedAt);
    }

    public static User create(String id, String givenNames, String familyName, String email, String password, String hint) {
        Instant now = Instant.now();
        return new User(id, givenNames, familyName, email, password, hint, Status.ACTIVE, now, now);
    }

    public String getName() {
        return String.join(" ", givenNames, familyName);
    }
}
