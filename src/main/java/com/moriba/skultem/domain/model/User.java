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

    // Shared across every role this account holds (Teacher, Parent, a plain Admin/Accountant/
    // Proprietor account) - it's a property of the person, not any one role. Optional - null until
    // uploaded, and can be added or replaced any time after the account is created.
    private String photo;

    // Whether the person has proved they own `email` (by opening a verification link sent to it), and when. Belongs to
    // the address, not the person: any change of email clears it. Accounts that existed before verification are
    // simply unverified - they keep full access.
    private boolean emailVerified;
    private Instant emailVerifiedAt;

    public enum Status {
        ACTIVE,
        RESET_PASSWORD,
        INACTIVE,
        DELETED
    }

    public User(String id, String givenNames, String familyName, String email, String password, String hint,
            Status status, String photo, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.givenNames = givenNames;
        this.familyName = familyName;
        this.email = email;
        this.password = password;
        this.hint = hint;
        this.status = status;
        this.photo = photo;
        touch(updatedAt);
    }

    public static User create(String givenNames, String familyName, String email, String password,
            String hint) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        return new User(id, givenNames, familyName, email, password, hint, Status.RESET_PASSWORD, null, now, now);
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

    public void setPhoto(String photo) {
        this.photo = photo;
        touch(Instant.now());
    }

    // For a User provisioned without an email (e.g. a parent added during enrollment whose
    // guardian had none) - an admin fills it in later, which is what actually grants portal
    // access since login is keyed on email. Refuses to overwrite an email that's already set;
    // changing an existing email is a different operation with different implications (it's also
    // the account's login) and isn't what this is for.
    public void updateEmail(String email) {
        if (this.email != null && !this.email.isBlank()) {
            throw new RuleException("This account already has an email set");
        }

        this.email = email;
        clearEmailVerification();
        touch(Instant.now());
    }

    // Replaces an email that is already set - unlike updateEmail this changes what the person logs in
    // with, so callers must be sure the account isn't shared (see EditParentUseCase).
    public void changeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RuleException("Email can't be empty");
        }

        if (this.email == null || !this.email.equalsIgnoreCase(email)) {
            // A new address hasn't been proved yet - verifying the old one says nothing about it.
            clearEmailVerification();
        }
        this.email = email;
        touch(Instant.now());
    }

    // Loading from storage.
    public void restoreEmailVerification(boolean verified, Instant verifiedAt) {
        this.emailVerified = verified;
        this.emailVerifiedAt = verified ? verifiedAt : null;
    }

    // The person opened a verification link sent to `verifiedEmail`. Only counts if it is still their address.
    public void markEmailVerified(String verifiedEmail, Instant at) {
        if (this.email == null || verifiedEmail == null || !this.email.equalsIgnoreCase(verifiedEmail)) {
            throw new RuleException("This link was sent to a different email address");
        }
        this.emailVerified = true;
        this.emailVerifiedAt = at;
        touch(at);
    }

    private void clearEmailVerification() {
        this.emailVerified = false;
        this.emailVerifiedAt = null;
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

    // An admin issuing a brand new temporary password on this user's behalf - e.g. they're
    // locked out and can't reach the self-service resetPassword() flow above (which requires
    // already being in RESET_PASSWORD state to use). Works from any state and puts the account
    // into RESET_PASSWORD, exactly like a freshly created account (see create()), so the admin
    // can hand the plaintext temp password to them and they're forced through /reset-password
    // the next time they log in.
    // Self-service "forgot password": the person proved they own the email, so the new password replaces whatever was
    // there (a temporary one included) and the account is ready to use.
    public void setNewPassword(String passwordHash) {
        this.password = passwordHash;
        this.hint = "";
        if (status == Status.RESET_PASSWORD) {
            this.status = Status.ACTIVE;
        }
        touch(Instant.now());
    }

    // A signed-in user changing their own password (they proved the current one first). Only an active
    // account - one still on a temporary password goes through resetPassword() instead.
    public void changePassword(String passwordHash) {
        if (status != Status.ACTIVE) {
            throw new RuleException("Set your password from the reset page first");
        }
        this.password = passwordHash;
        this.hint = "";
        touch(Instant.now());
    }

    public void issueTemporaryPassword(String passwordHash, String hint) {
        this.password = passwordHash;
        this.hint = hint;
        this.status = Status.RESET_PASSWORD;
        touch(Instant.now());
    }
}
