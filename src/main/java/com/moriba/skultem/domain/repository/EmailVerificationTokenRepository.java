package com.moriba.skultem.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.EmailVerificationToken;

public interface EmailVerificationTokenRepository {

    void save(EmailVerificationToken token);

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    // Links not yet used or replaced - the ones a new link revokes.
    List<EmailVerificationToken> findActiveByUserId(String userId);

    // How many links were sent to this account since `since` (rate limiting).
    long countByUserIdSince(String userId, Instant since);

    Optional<EmailVerificationToken> findLatestByUserId(String userId);
}
