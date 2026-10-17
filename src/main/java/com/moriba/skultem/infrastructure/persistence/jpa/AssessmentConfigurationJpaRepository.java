package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.AssessmentConfigurationEntity;

public interface AssessmentConfigurationJpaRepository extends JpaRepository<AssessmentConfigurationEntity, String> {
    Optional<AssessmentConfigurationEntity> findBySchoolIdAndManagementSectionIdIsNullAndAssessmentTemplateIdIsNull(
            String schoolId);

    Optional<AssessmentConfigurationEntity> findBySchoolIdAndManagementSectionIdAndAssessmentTemplateIdIsNull(
            String schoolId, String sectionId);

    Optional<AssessmentConfigurationEntity> findBySchoolIdAndAssessmentTemplateId(String schoolId, String templateId);

    List<AssessmentConfigurationEntity> findAllBySchoolId(String schoolId);
}
