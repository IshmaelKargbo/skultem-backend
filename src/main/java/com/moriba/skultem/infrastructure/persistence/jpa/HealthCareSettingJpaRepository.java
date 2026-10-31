package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.HealthCareSettingEntity;

public interface HealthCareSettingJpaRepository extends JpaRepository<HealthCareSettingEntity, String> {

    Optional<HealthCareSettingEntity> findBySchoolId(String schoolId);
}
