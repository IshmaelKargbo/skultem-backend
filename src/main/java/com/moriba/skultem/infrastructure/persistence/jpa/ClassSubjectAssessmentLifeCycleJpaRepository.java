package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.ClassSubjectAssessmentLifeCycleEntity;

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

        // Resolves by the real subject + class session rather than one specific TeacherSubject row
        // - see StudentAssessmentJpaRepository#findAllBySubjectAndSessionAndTermId for why: a
        // subject can have more than one teacher, and they must all reach the same cycle/gradebook.
        @Query("""
                                select c from ClassSubjectAssessmentLifeCycleEntity c
                                where c.subject.subject.id = :subjectId
                                and c.subject.session.id = :sessionId
                                and c.assessment.id = :assessmentId
                                and c.term.id = :termId
                        """)
        Optional<ClassSubjectAssessmentLifeCycleEntity> findBySubjectSessionAssessmentAndTerm(
                        @Param("subjectId") String subjectId,
                        @Param("sessionId") String sessionId,
                        @Param("assessmentId") String assessmentId,
                        @Param("termId") String termId);
}
