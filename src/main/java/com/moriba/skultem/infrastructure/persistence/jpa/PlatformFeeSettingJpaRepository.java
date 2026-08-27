package com.moriba.skultem.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.PlatformFeeSettingEntity;

public interface PlatformFeeSettingJpaRepository extends JpaRepository<PlatformFeeSettingEntity, String> {
}
