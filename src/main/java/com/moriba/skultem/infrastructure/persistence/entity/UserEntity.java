package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import com.moriba.skultem.domain.model.User.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String givenName;

    @Column(nullable = false)
    private String familyName;

    // Nullable - a parent created without an email (some don't have one) gets a User row with no
    // email until an admin adds one later via AddParentEmailUseCase, which is also what grants
    // them portal access (login is keyed on email, so no email means no way to authenticate yet).
    private String email;

    @Column(nullable = false)
    private String password;

    private String hint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String photo;

    // See User#emailVerified - defaults to false for every existing account.
    @Column(name = "email_verified", nullable = false)
    @lombok.Builder.Default
    private boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    private Instant createdAt;
    private Instant updatedAt;
}
