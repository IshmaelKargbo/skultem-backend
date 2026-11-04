package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

// One "forgot password" link. Holds only the hash of the token. Usable once, until it expires or a newer link
// replaces it.
@Getter
public class PasswordResetToken {
    private final String id;
    private final String userId;
    private final String tokenHash;
    private final String schoolId;
    private final Instant expiresAt;
    private Instant usedAt;
    private Instant revokedAt;
    private final Instant createdAt;

    public PasswordResetToken(String id, String userId, String tokenHash, String schoolId, Instant expiresAt,
            Instant usedAt, Instant revokedAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.schoolId = schoolId;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.revokedAt = revokedAt;
        this.createdAt = createdAt;
    }

    public static PasswordResetToken issue(String userId, String tokenHash, String schoolId, Instant now,
            Instant expiresAt) {
        return new PasswordResetToken(UUID.randomUUID().toString(), userId, tokenHash, schoolId, expiresAt, null, null,
                now);
    }

    // Still usable: not used, not replaced, not expired.
    public boolean isUsable(Instant now) {
        return usedAt == null && revokedAt == null && now.isBefore(expiresAt);
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
