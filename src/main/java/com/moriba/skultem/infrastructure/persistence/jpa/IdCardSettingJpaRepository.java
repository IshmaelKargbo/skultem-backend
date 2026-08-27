package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.IdCardSettingEntity;

public interface IdCardSettingJpaRepository extends JpaRepository<IdCardSettingEntity, String> {
    Optional<IdCardSettingEntity> findBySchoolId(String schoolId);
}
