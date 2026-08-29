package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ReceiptSettingEntity;

public interface ReceiptSettingJpaRepository extends JpaRepository<ReceiptSettingEntity, String> {
    Optional<ReceiptSettingEntity> findBySchoolId(String schoolId);
}
