package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.infrastructure.persistence.entity.MobileDeviceEntity;

public interface MobileDeviceJpaRepository extends JpaRepository<MobileDeviceEntity, String> {
    Optional<MobileDeviceEntity> findByToken(String token);

    List<MobileDeviceEntity> findAllByUserId(String userId);

    @Transactional
    void deleteByToken(String token);
}
