package com.moriba.skultem.infrastructure.persistence.adapter;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.repository.EarlierTermAssessmentCleanupRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class EarlierTermAssessmentCleanupAdapter implements EarlierTermAssessmentCleanupRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public int deleteUngradedBeforeTerm(String schoolId, String templateId, String academicYearId, int termNumber) {
        // Scores first (nothing marked, no CA recorded), then the student assessments and tests that are left empty.
        int scores = em.createQuery("""
                DELETE FROM AssessmentScoreEntity s
                WHERE s.gradedByUserId IS NULL
                  AND s.studentAssessment.id IN (
                      SELECT a.id FROM StudentAssessmentEntity a
                      WHERE a.schoolId = :schoolId
                        AND a.enrollment.id IN (SELECT e.id FROM EnrollmentEntity e
                                                WHERE e.clazz.id IN (SELECT c.id FROM ClassEntity c WHERE c.template.id = :templateId) AND e.academicYear.id = :yearId)
                        AND a.term.id IN (SELECT t.id FROM TermEntity t
                                          WHERE t.academicYear.id = :yearId AND t.termNumber < :termNumber))
                  AND NOT EXISTS (SELECT 1 FROM AssessmentCaEntryEntity ce WHERE ce.assessmentScoreId = s.id)
                """)
                .setParameter("schoolId", schoolId).setParameter("templateId", templateId)
                .setParameter("yearId", academicYearId).setParameter("termNumber", termNumber)
                .executeUpdate();

        em.createQuery("""
                DELETE FROM StudentAssessmentEntity a
                WHERE a.schoolId = :schoolId
                  AND a.enrollment.id IN (SELECT e.id FROM EnrollmentEntity e
                                          WHERE e.clazz.id IN (SELECT c.id FROM ClassEntity c WHERE c.template.id = :templateId) AND e.academicYear.id = :yearId)
                  AND a.term.id IN (SELECT t.id FROM TermEntity t
                                    WHERE t.academicYear.id = :yearId AND t.termNumber < :termNumber)
                  AND NOT EXISTS (SELECT 1 FROM AssessmentScoreEntity s WHERE s.studentAssessment.id = a.id)
                """)
                .setParameter("schoolId", schoolId).setParameter("templateId", templateId)
                .setParameter("yearId", academicYearId).setParameter("termNumber", termNumber)
                .executeUpdate();

        em.createQuery("""
                DELETE FROM ClassSubjectAssessmentLifeCycleEntity c
                WHERE c.schoolId = :schoolId
                  AND c.term.id IN (SELECT t.id FROM TermEntity t
                                    WHERE t.academicYear.id = :yearId AND t.termNumber < :termNumber)
                  AND c.subject.id IN (SELECT ts.id FROM TeacherSubjectEntity ts
                                       WHERE ts.session.id IN (SELECT cs.id FROM ClassSessionEntity cs
                                                               WHERE cs.clazz.id IN (SELECT c.id FROM ClassEntity c WHERE c.template.id = :templateId) AND cs.academicYear.id = :yearId))
                  AND NOT EXISTS (SELECT 1 FROM AssessmentScoreEntity s WHERE s.cycle.id = c.id)
                """)
                .setParameter("schoolId", schoolId).setParameter("templateId", templateId)
                .setParameter("yearId", academicYearId).setParameter("termNumber", termNumber)
                .executeUpdate();

        em.flush();
        em.clear();
        return scores;
    }
}
