package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.LibrarySettingEntity;

public interface LibrarySettingJpaRepository extends JpaRepository<LibrarySettingEntity, String> {
    Optional<LibrarySettingEntity> findBySchoolId(String schoolId);
}
