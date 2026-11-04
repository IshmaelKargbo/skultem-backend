package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.PasswordResetTokenEntity;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, String> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    List<PasswordResetTokenEntity> findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(String userId);

    long countByUserIdAndCreatedAtAfter(String userId, Instant since);

    Optional<PasswordResetTokenEntity> findFirstByUserIdOrderByCreatedAtDesc(String userId);
}
