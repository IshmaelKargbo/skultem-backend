package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.PromotionConfigEntity;

public interface PromotionConfigJpaRepository extends JpaRepository<PromotionConfigEntity, String> {
    Optional<PromotionConfigEntity> findBySchoolId(String schoolId);
}
