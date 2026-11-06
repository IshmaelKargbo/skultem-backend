package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ExamTimetableEntity;

public interface ExamTimetableJpaRepository extends JpaRepository<ExamTimetableEntity, String> {
    Optional<ExamTimetableEntity> findByIdAndSchoolId(String id, String schoolId);

    Optional<ExamTimetableEntity> findBySchoolIdAndTermIdAndAssessmentId(String schoolId, String termId,
            String assessmentId);

    List<ExamTimetableEntity> findAllBySchoolIdAndTermId(String schoolId, String termId);
}
