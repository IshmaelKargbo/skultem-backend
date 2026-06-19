package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.application.error.RuleException;
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
        RESET_PASSWORD,
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

    public static User create(String givenNames, String familyName, String email, String password,
            String hint) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        return new User(id, givenNames, familyName, email, password, hint, Status.RESET_PASSWORD, now, now);
    }

    public void update(String givenNames, String familyName) {
        if (givenNames != null) {
            this.givenNames = givenNames;
        }

        if (familyName != null) {
            this.familyName = familyName;
        }

        touch(Instant.now());
    }

    public String getName() {
        return String.join(" ", givenNames, familyName);
    }

    public void resetPassword(String password) {
        if (status != Status.RESET_PASSWORD) {
            throw new RuleException("your account must be RESET_PASSWORD state in other to use the feature");
        }

        this.password = password;
        this.hint = "";
        this.status = Status.ACTIVE;
        touch(Instant.now());
    }
}
