package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.ClassSubjectAssessmentLifeCycleEntity;

@Repository
public interface ClassSubjectAssessmentLifeCycleJpaRepository
                extends JpaRepository<ClassSubjectAssessmentLifeCycleEntity, String> {
        boolean existsByTerm_IdAndAssessment_IdAndSubject_Id(String termId, String assessmentId,
                        String teacherSubjectId);

        List<ClassSubjectAssessmentLifeCycleEntity> findAllBySubject_IdAndTerm_IdOrderByAssessment_PositionAsc(
                        String subjectId, String termId);

        List<ClassSubjectAssessmentLifeCycleEntity> findAllBySchoolIdAndTerm_IdOrderByAssessment_PositionAsc(
                        String schoolId, String termId);

        List<ClassSubjectAssessmentLifeCycleEntity> findAllBySchoolIdAndTerm_IdAndAssessment_PositionOrderBySubject_Id(
                        String schoolId, String termId, int position);

        List<ClassSubjectAssessmentLifeCycleEntity> findAllBySchoolIdAndTerm_IdAndSubject_Session_Clazz_Id(
                        String schoolId, String termId, String classId);

        Optional<ClassSubjectAssessmentLifeCycleEntity> findBySubject_IdAndAssessment_IdAndTerm_Id(
                        String subjectId, String assessmentId, String termId);
}
