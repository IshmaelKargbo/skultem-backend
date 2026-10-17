package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.OnboardingEntity;

public interface OnboardingJpaRepository extends JpaRepository<OnboardingEntity, UUID> {
    Optional<OnboardingEntity> findByLeadId(UUID leadId);
}
