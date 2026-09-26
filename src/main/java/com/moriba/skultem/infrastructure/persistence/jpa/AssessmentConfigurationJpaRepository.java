package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.AssessmentConfigurationEntity;

public interface AssessmentConfigurationJpaRepository extends JpaRepository<AssessmentConfigurationEntity, String> {
    Optional<AssessmentConfigurationEntity> findBySchoolIdAndManagementSectionIdIsNull(String schoolId);

    Optional<AssessmentConfigurationEntity> findBySchoolIdAndManagementSectionId(String schoolId, String sectionId);

    List<AssessmentConfigurationEntity> findAllBySchoolId(String schoolId);
}
