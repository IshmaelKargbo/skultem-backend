package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.AssessmentTemplateEntity;

public interface AssessmentTemplateJpaRepository extends JpaRepository<AssessmentTemplateEntity, String> {
    Optional<AssessmentTemplateEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<AssessmentTemplateEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);
}
