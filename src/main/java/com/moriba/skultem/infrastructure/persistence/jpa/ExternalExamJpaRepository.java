package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ExternalExamEntity;

public interface ExternalExamJpaRepository extends JpaRepository<ExternalExamEntity, String> {
    List<ExternalExamEntity> findAllBySchoolIdOrderByNameAsc(String schoolId);

    Optional<ExternalExamEntity> findByIdAndSchoolId(String id, String schoolId);

    boolean existsBySchoolIdAndCode(String schoolId, String code);
}
