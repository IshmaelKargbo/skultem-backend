package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.AssessmentConfiguration;

public interface AssessmentConfigurationRepository {
    void save(AssessmentConfiguration domain);

    // sectionId null = the school-wide configuration.
    Optional<AssessmentConfiguration> findBySection(String schoolId, String sectionId);

    List<AssessmentConfiguration> findAllBySchool(String schoolId);
}
