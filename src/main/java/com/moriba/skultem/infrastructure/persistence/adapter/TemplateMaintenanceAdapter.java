package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle.Status;
import com.moriba.skultem.domain.repository.TemplateMaintenanceRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class TemplateMaintenanceAdapter implements TemplateMaintenanceRepository {

    private static final List<Status> SUBMITTED = List.of(Status.SUBMITTED, Status.APPROVED, Status.COMPLETED);

    @PersistenceContext
    private EntityManager em;

    @Override
    public Usage usage(String schoolId, String templateId) {
        List<String> classes = em.createQuery("""
                SELECT c.name FROM ClassEntity c
                WHERE c.schoolId = :schoolId AND c.template.id = :templateId AND c.status = 'ACTIVE'
                ORDER BY c.levelOrder
                """, String.class)
                .setParameter("schoolId", schoolId).setParameter("templateId", templateId).getResultList();

        // Submitted, approved or completed - or locked after approval, which shows as marks someone entered.
        Long submitted = em.createQuery("""
                SELECT COUNT(l) FROM ClassSubjectAssessmentLifeCycleEntity l
                WHERE l.assessment.template.id = :templateId
                  AND (l.status IN :submitted
                       OR (l.status = :locked AND EXISTS (SELECT 1 FROM AssessmentScoreEntity s
                                                          WHERE s.cycle.id = l.id AND s.gradedByUserId IS NOT NULL)))
                """, Long.class)
                .setParameter("templateId", templateId).setParameter("submitted", SUBMITTED)
                .setParameter("locked", Status.LOCKED).getSingleResult();

        Long marked = em.createQuery("""
                SELECT COUNT(s) FROM AssessmentScoreEntity s
                WHERE s.cycle.assessment.template.id = :templateId AND (s.score > 0 OR s.gradedByUserId IS NOT NULL)
                """, Long.class)
                .setParameter("templateId", templateId).getSingleResult();

        return new Usage(classes, submitted, marked);
    }

    @Override
    public long markedScoresOf(String assessmentId) {
        return em.createQuery("""
                SELECT COUNT(s) FROM AssessmentScoreEntity s
                WHERE s.cycle.assessment.id = :assessmentId AND (s.score > 0 OR s.gradedByUserId IS NOT NULL)
                """, Long.class)
                .setParameter("assessmentId", assessmentId).getSingleResult();
    }

    @Override
    public int updateScoreWeight(String assessmentId, int weight) {
        int n = em.createQuery("""
                UPDATE AssessmentScoreEntity s SET s.weight = :weight
                WHERE s.cycle.id IN (SELECT l.id FROM ClassSubjectAssessmentLifeCycleEntity l
                                     WHERE l.assessment.id = :assessmentId)
                """)
                .setParameter("weight", weight).setParameter("assessmentId", assessmentId).executeUpdate();
        em.flush();
        em.clear();
        return n;
    }

    @Override
    public void deleteAssessmentCascade(String assessmentId) {
        em.createQuery("""
                DELETE FROM AssessmentCaEntryEntity ce WHERE ce.assessmentScoreId IN (
                    SELECT s.id FROM AssessmentScoreEntity s
                    WHERE s.cycle.id IN (SELECT l.id FROM ClassSubjectAssessmentLifeCycleEntity l
                                         WHERE l.assessment.id = :assessmentId))
                """).setParameter("assessmentId", assessmentId).executeUpdate();
        em.createQuery("""
                DELETE FROM AssessmentScoreEntity s
                WHERE s.cycle.id IN (SELECT l.id FROM ClassSubjectAssessmentLifeCycleEntity l
                                     WHERE l.assessment.id = :assessmentId)
                """).setParameter("assessmentId", assessmentId).executeUpdate();
        em.createQuery("""
                DELETE FROM AssessmentApprovalRequestEntity r
                WHERE r.cycle.id IN (SELECT l.id FROM ClassSubjectAssessmentLifeCycleEntity l
                                     WHERE l.assessment.id = :assessmentId)
                """).setParameter("assessmentId", assessmentId).executeUpdate();
        em.createQuery("DELETE FROM ClassSubjectAssessmentLifeCycleEntity l WHERE l.assessment.id = :assessmentId")
                .setParameter("assessmentId", assessmentId).executeUpdate();
        em.createQuery("DELETE FROM AssessmentEntity a WHERE a.id = :assessmentId")
                .setParameter("assessmentId", assessmentId).executeUpdate();
        em.flush();
        em.clear();
    }

    @Override
    public long markedScoresOf(String assessmentId, java.util.Collection<Integer> termNumbers) {
        return em.createQuery("""
                SELECT COUNT(s) FROM AssessmentScoreEntity s
                WHERE s.cycle.assessment.id = :assessmentId AND s.cycle.term.termNumber IN :terms
                  AND (s.score > 0 OR s.gradedByUserId IS NOT NULL)
                """, Long.class)
                .setParameter("assessmentId", assessmentId).setParameter("terms", termNumbers).getSingleResult();
    }

    @Override
    public void deleteAssessmentInTerms(String assessmentId, java.util.Collection<Integer> termNumbers) {
        List<String> cycleIds = em.createQuery("""
                SELECT l.id FROM ClassSubjectAssessmentLifeCycleEntity l
                WHERE l.assessment.id = :assessmentId AND l.term.termNumber IN :terms
                """, String.class)
                .setParameter("assessmentId", assessmentId).setParameter("terms", termNumbers).getResultList();
        if (cycleIds.isEmpty()) {
            return;
        }
        em.createQuery("""
                DELETE FROM AssessmentCaEntryEntity ce WHERE ce.assessmentScoreId IN (
                    SELECT s.id FROM AssessmentScoreEntity s WHERE s.cycle.id IN :cycles)
                """).setParameter("cycles", cycleIds).executeUpdate();
        em.createQuery("DELETE FROM AssessmentScoreEntity s WHERE s.cycle.id IN :cycles")
                .setParameter("cycles", cycleIds).executeUpdate();
        em.createQuery("DELETE FROM AssessmentApprovalRequestEntity r WHERE r.cycle.id IN :cycles")
                .setParameter("cycles", cycleIds).executeUpdate();
        em.createQuery("DELETE FROM ClassSubjectAssessmentLifeCycleEntity l WHERE l.id IN :cycles")
                .setParameter("cycles", cycleIds).executeUpdate();
        em.flush();
        em.clear();
    }
}
