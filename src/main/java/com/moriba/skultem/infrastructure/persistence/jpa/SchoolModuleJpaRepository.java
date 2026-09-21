package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.SchoolModuleEntity;

public interface SchoolModuleJpaRepository extends JpaRepository<SchoolModuleEntity, String> {
    List<SchoolModuleEntity> findAllBySchoolIdAndEnabledTrue(String schoolId);

    Optional<SchoolModuleEntity> findBySchoolIdAndModuleKey(String schoolId, String moduleKey);
}
