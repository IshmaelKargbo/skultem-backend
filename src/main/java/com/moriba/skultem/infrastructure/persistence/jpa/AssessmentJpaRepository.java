package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.AssessmentEntity;

public interface AssessmentJpaRepository extends JpaRepository<AssessmentEntity, String> {
    List<AssessmentEntity> findAllByTemplate_IdAndSchoolIdOrderByPositionAsc(String templateId, String schoolId);
    
    List<AssessmentEntity> findAllBySchoolIdOrderByPositionAsc(String schoolId);

    void deleteAllByTemplate_IdAndSchoolId(String templateId, String schoolId);
}
