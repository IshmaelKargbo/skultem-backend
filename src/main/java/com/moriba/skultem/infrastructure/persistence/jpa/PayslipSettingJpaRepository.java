package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.PayslipSettingEntity;

public interface PayslipSettingJpaRepository extends JpaRepository<PayslipSettingEntity, String> {
    Optional<PayslipSettingEntity> findBySchoolId(String schoolId);
}
