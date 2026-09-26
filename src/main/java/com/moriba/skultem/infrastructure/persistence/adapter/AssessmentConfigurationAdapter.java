package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.AssessmentConfiguration;
import com.moriba.skultem.domain.repository.AssessmentConfigurationRepository;
import com.moriba.skultem.infrastructure.persistence.entity.AssessmentConfigurationEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.AssessmentConfigurationJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AssessmentConfigurationAdapter implements AssessmentConfigurationRepository {

    private final AssessmentConfigurationJpaRepository repo;

    @Override
    public void save(AssessmentConfiguration d) {
        repo.save(AssessmentConfigurationEntity.builder()
                .id(d.getId())
                .schoolId(d.getSchoolId())
                .managementSectionId(d.getManagementSectionId())
                .structure(d.getStructure())
                .caPercentage(d.getCaPercentage())
                .formalPercentage(d.getFormalPercentage())
                .caFrequency(d.getCaFrequency())
                .caEntries(d.getCaEntries())
                .version(d.getVersion())
                .updatedByUserId(d.getUpdatedByUserId())
                .planText(d.planAsText())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build());
    }

    @Override
    public Optional<AssessmentConfiguration> findBySection(String schoolId, String sectionId) {
        var found = sectionId == null
                ? repo.findBySchoolIdAndManagementSectionIdIsNull(schoolId)
                : repo.findBySchoolIdAndManagementSectionId(schoolId, sectionId);
        return found.map(AssessmentConfigurationAdapter::toDomain);
    }

    @Override
    public List<AssessmentConfiguration> findAllBySchool(String schoolId) {
        return repo.findAllBySchoolId(schoolId).stream().map(AssessmentConfigurationAdapter::toDomain).toList();
    }

    private static AssessmentConfiguration toDomain(AssessmentConfigurationEntity e) {
        var config = new AssessmentConfiguration(e.getId(), e.getSchoolId(), e.getManagementSectionId(),
                e.getStructure(), e.getCaPercentage(), e.getFormalPercentage(), e.getCaFrequency(), e.getCaEntries(),
                e.getVersion(), e.getUpdatedByUserId(), e.getCreatedAt(), e.getUpdatedAt());
        config.restorePlan(e.getPlanText());
        return config;
    }
}
