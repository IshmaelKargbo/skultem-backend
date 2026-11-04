package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.EmailVerificationTokenEntity;

public interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationTokenEntity, String> {

    Optional<EmailVerificationTokenEntity> findByTokenHash(String tokenHash);

    List<EmailVerificationTokenEntity> findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(String userId);

    long countByUserIdAndCreatedAtAfter(String userId, Instant since);

    Optional<EmailVerificationTokenEntity> findFirstByUserIdOrderByCreatedAtDesc(String userId);
}
