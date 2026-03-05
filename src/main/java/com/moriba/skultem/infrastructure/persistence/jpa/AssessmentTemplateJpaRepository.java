package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.AssessmentTemplateEntity;

@Repository
public interface AssessmentTemplateJpaRepository extends JpaRepository<AssessmentTemplateEntity, String> {
    Optional<AssessmentTemplateEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<AssessmentTemplateEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);
}
