package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.infrastructure.persistence.entity.AssessmentScoreEntity;

@Repository
public interface AssessmentScoreJpaRepository extends JpaRepository<AssessmentScoreEntity, String> {
    List<AssessmentScoreEntity> findAllByStudentAssessment_IdOrderByCycle_Assessment_PositionAsc(String assessmentId);

    List<AssessmentScoreEntity> findAllByStudentAssessment_IdAndCycle_IdOrderByCycle_Assessment_PositionAsc(String studentAssessmentId, String assessmentId);

    List<AssessmentScoreEntity> findAllByStudentAssessment_IdAndCycle_Assessment_IdOrderByCycle_Assessment_PositionAsc(
            String studentAssessmentId, String assessmentId);

    List<AssessmentScoreEntity> findAllByCycle_Id(String cycleId);

    boolean existsByStudentAssessment_IdAndCycle_Id(String studentAssessmentId, String cycleId);

    boolean existsByStudentAssessment_Enrollment_Clazz_IdAndSchoolIdAndScoreGreaterThan(
            String classId,
            String schoolId,
            Integer score);

    boolean existsByStudentAssessment_Enrollment_Clazz_IdAndSchoolIdAndCycle_StatusNot(
            String classId,
            String schoolId,
            ClassSubjectAssessmentLifeCycle.Status status);

    boolean existsByStudentAssessment_Enrollment_Clazz_IdAndStudentAssessment_TeacherSubject_Subject_IdAndSchoolIdAndScoreGreaterThan(
            String classId,
            String subjectId,
            String schoolId,
            Integer score);

    boolean existsByStudentAssessment_Enrollment_Clazz_IdAndStudentAssessment_TeacherSubject_Subject_IdAndSchoolIdAndCycle_StatusNot(
            String classId,
            String subjectId,
            String schoolId,
            ClassSubjectAssessmentLifeCycle.Status status);
    
}
