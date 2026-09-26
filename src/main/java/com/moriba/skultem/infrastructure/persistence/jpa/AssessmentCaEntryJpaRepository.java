package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.AssessmentCaEntryEntity;

public interface AssessmentCaEntryJpaRepository extends JpaRepository<AssessmentCaEntryEntity, String> {
    List<AssessmentCaEntryEntity> findAllByAssessmentScoreIdIn(Collection<String> assessmentScoreIds);
}
