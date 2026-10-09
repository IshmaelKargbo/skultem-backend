package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.TimetableSettingEntity;

public interface TimetableSettingJpaRepository extends JpaRepository<TimetableSettingEntity, String> {
    Optional<TimetableSettingEntity> findBySchoolId(String schoolId);
}
