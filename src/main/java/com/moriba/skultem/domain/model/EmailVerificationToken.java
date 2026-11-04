package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

// One verification link sent to an address. Holds only the hash of the token. Usable once, until it expires or a
// newer link replaces it.
@Getter
public class EmailVerificationToken {

    private final String id;
    private final String userId;
    private final String email;
    private final String tokenHash;
    private final String schoolId;
    private final Instant expiresAt;
    private Instant usedAt;
    private Instant revokedAt;
    private final Instant createdAt;

    public EmailVerificationToken(String id, String userId, String email, String tokenHash, String schoolId,
            Instant expiresAt, Instant usedAt, Instant revokedAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.tokenHash = tokenHash;
        this.schoolId = schoolId;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.revokedAt = revokedAt;
        this.createdAt = createdAt;
    }

    public static EmailVerificationToken issue(String userId, String email, String tokenHash, String schoolId,
            Instant now, Instant expiresAt) {
        return new EmailVerificationToken(UUID.randomUUID().toString(), userId, email, tokenHash, schoolId, expiresAt,
                null, null, now);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void markUsed(Instant at) {
        this.usedAt = at;
    }

    public void revoke(Instant at) {
        if (usedAt == null && revokedAt == null) {
            this.revokedAt = at;
        }
    }
}
