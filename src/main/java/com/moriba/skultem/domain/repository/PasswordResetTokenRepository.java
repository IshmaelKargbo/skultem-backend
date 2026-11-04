package com.moriba.skultem.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.PasswordResetToken;

public interface PasswordResetTokenRepository {
    void save(PasswordResetToken token);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // Links not yet used or replaced - the ones a new link (or a completed reset) revokes.
    List<PasswordResetToken> findActiveByUserId(String userId);

    // How many links were sent to this account since `since` (rate limiting).
    long countByUserIdSince(String userId, Instant since);

    Optional<PasswordResetToken> findLatestByUserId(String userId);
}
