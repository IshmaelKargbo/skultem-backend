package com.moriba.skultem.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.PasswordResetToken;
import com.moriba.skultem.domain.repository.PasswordResetTokenRepository;
import com.moriba.skultem.infrastructure.persistence.entity.PasswordResetTokenEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.PasswordResetTokenJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository repo;

    @Override
    public void save(PasswordResetToken t) {
        repo.save(new PasswordResetTokenEntity(t.getId(), t.getUserId(), t.getTokenHash(), t.getSchoolId(),
                t.getExpiresAt(), t.getUsedAt(), t.getRevokedAt(), t.getCreatedAt()));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return repo.findByTokenHash(tokenHash).map(PasswordResetTokenAdapter::toDomain);
    }

    @Override
    public List<PasswordResetToken> findActiveByUserId(String userId) {
        return repo.findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(userId).stream()
                .map(PasswordResetTokenAdapter::toDomain).toList();
    }

    @Override
    public long countByUserIdSince(String userId, Instant since) {
        return repo.countByUserIdAndCreatedAtAfter(userId, since);
    }

    @Override
    public Optional<PasswordResetToken> findLatestByUserId(String userId) {
        return repo.findFirstByUserIdOrderByCreatedAtDesc(userId).map(PasswordResetTokenAdapter::toDomain);
    }

    private static PasswordResetToken toDomain(PasswordResetTokenEntity e) {
        return new PasswordResetToken(e.getId(), e.getUserId(), e.getTokenHash(), e.getSchoolId(), e.getExpiresAt(),
                e.getUsedAt(), e.getRevokedAt(), e.getCreatedAt());
    }
}
