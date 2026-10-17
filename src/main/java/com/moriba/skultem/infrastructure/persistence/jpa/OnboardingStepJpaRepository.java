package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.OnboardingStepEntity;

public interface OnboardingStepJpaRepository extends JpaRepository<OnboardingStepEntity, UUID> {
    List<OnboardingStepEntity> findByOnboardingIdOrderByPosition(UUID onboardingId);
}
