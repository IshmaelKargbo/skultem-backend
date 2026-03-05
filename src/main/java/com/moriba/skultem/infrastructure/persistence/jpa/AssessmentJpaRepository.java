package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.AssessmentEntity;

@Repository
public interface AssessmentJpaRepository extends JpaRepository<AssessmentEntity, String> {
    List<AssessmentEntity> findAllByTemplate_IdAndSchoolIdOrderByPositionAsc(String templateId, String schoolId);

    void deleteAllByTemplate_IdAndSchoolId(String templateId, String schoolId);
}
