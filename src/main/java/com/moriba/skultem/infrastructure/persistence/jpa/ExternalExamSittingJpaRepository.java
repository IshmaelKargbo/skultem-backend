package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ExternalExamSittingEntity;

public interface ExternalExamSittingJpaRepository extends JpaRepository<ExternalExamSittingEntity, String> {
    List<ExternalExamSittingEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId);

    Optional<ExternalExamSittingEntity> findByIdAndSchoolId(String id, String schoolId);

    boolean existsBySchoolIdAndExamIdAndClassIdAndAcademicYearId(String schoolId, String examId, String classId,
            String academicYearId);

    boolean existsByExamId(String examId);
}
