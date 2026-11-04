package com.moriba.skultem.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.EmailVerificationToken;
import com.moriba.skultem.domain.repository.EmailVerificationTokenRepository;
import com.moriba.skultem.infrastructure.persistence.entity.EmailVerificationTokenEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.EmailVerificationTokenJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EmailVerificationTokenAdapter implements EmailVerificationTokenRepository {

    private final EmailVerificationTokenJpaRepository repo;

    @Override
    public void save(EmailVerificationToken t) {
        repo.save(new EmailVerificationTokenEntity(t.getId(), t.getUserId(), t.getEmail(), t.getTokenHash(),
                t.getSchoolId(), t.getExpiresAt(), t.getUsedAt(), t.getRevokedAt(), t.getCreatedAt()));
    }

    @Override
    public Optional<EmailVerificationToken> findByTokenHash(String tokenHash) {
        return repo.findByTokenHash(tokenHash).map(EmailVerificationTokenAdapter::toDomain);
    }

    @Override
    public List<EmailVerificationToken> findActiveByUserId(String userId) {
        return repo.findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(userId).stream()
                .map(EmailVerificationTokenAdapter::toDomain).toList();
    }

    @Override
    public long countByUserIdSince(String userId, Instant since) {
        return repo.countByUserIdAndCreatedAtAfter(userId, since);
    }

    @Override
    public Optional<EmailVerificationToken> findLatestByUserId(String userId) {
        return repo.findFirstByUserIdOrderByCreatedAtDesc(userId).map(EmailVerificationTokenAdapter::toDomain);
    }

    private static EmailVerificationToken toDomain(EmailVerificationTokenEntity e) {
        return new EmailVerificationToken(e.getId(), e.getUserId(), e.getEmail(), e.getTokenHash(), e.getSchoolId(),
                e.getExpiresAt(), e.getUsedAt(), e.getRevokedAt(), e.getCreatedAt());
    }
}
