package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.AssessmentScoreEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

public interface AssessmentScoreJpaRepository
                extends JpaRepository<AssessmentScoreEntity, String>, JpaSpecificationExecutor<AssessmentScoreEntity> {
        List<AssessmentScoreEntity> findAllByStudentAssessment_IdOrderByCycle_Assessment_PositionAsc(
                        String assessmentId);

        List<AssessmentScoreEntity> findAllByStudentAssessment_IdAndCycle_IdOrderByCycle_Assessment_PositionAsc(
                        String studentAssessmentId, String assessmentId);

        List<AssessmentScoreEntity> findAllByStudentAssessment_IdAndCycle_Assessment_IdOrderByCycle_Assessment_PositionAsc(
                        String studentAssessmentId, String assessmentId);

        List<AssessmentScoreEntity> findAllByCycle_Id(String cycleId);

        List<AssessmentScoreEntity> findAllByStudentAssessment_Enrollment_IdAndSchoolId(String enrollmentId,
                        String schoolId);

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

        boolean existsByStudentAssessment_Enrollment_Clazz_IdAndStudentAssessment_TeacherSubject_Subject_IdAndStudentAssessment_Enrollment_AcademicYear_IdAndSchoolIdAndScoreGreaterThan(
                        String classId,
                        String subjectId,
                        String academicYearId,
                        String schoolId,
                        Integer score);

        boolean existsByStudentAssessment_Enrollment_Clazz_IdAndStudentAssessment_TeacherSubject_Subject_IdAndStudentAssessment_Enrollment_AcademicYear_IdAndSchoolIdAndCycle_StatusNot(
                        String classId,
                        String subjectId,
                        String academicYearId,
                        String schoolId,
                        ClassSubjectAssessmentLifeCycle.Status status);

        @Query("""
                        SELECT position FROM (
                            SELECT e.student.id as studentId,
                                   RANK() OVER (ORDER BY SUM(a.score * a.weight) DESC) as position
                            FROM AssessmentScoreEntity a
                            JOIN a.studentAssessment sa
                            JOIN a.cycle cy
                            JOIN cy.term t
                            JOIN sa.enrollment e
                            JOIN e.clazz c
                            WHERE a.schoolId = :schoolId
                            AND c.id = :classId
                            AND t.id = :termId
                            GROUP BY e.student.id
                        ) r
                        WHERE r.studentId = :studentId
                        """)
        Integer getStudentRank(
                        String schoolId,
                        String classId,
                        String termId,
                        String studentId);

        // Per-student average score (score is already 0-100, see AssessmentScore.calculateWeightedScore)
        // for one class in one term - backs the "needs attention" flag (ComputeClassAttentionUseCase).
        // A flat average across every assessment recorded so far, not the subject-weighted average
        // report cards use (GenerateReportCardsUseCase) - good enough for a heuristic, without needing
        // that heavier per-subject computation for every class on a list page.
        //
        // Excludes DRAFT and LOCKED cycles - a ClassSubjectAssessmentLifeCycle's score rows are
        // scaffolded for every student up front at score=0 before a teacher enters anything, and a
        // term that just started is mostly DRAFT cycles (the first assessment position) or LOCKED
        // ones (every later position, provisioned "not yet open" - see
        // ProvisionStudentAssessmentsUseCase - only DRAFT to SUBMITTED to ... to LOCKED once a
        // teacher actually advances through it). Without this filter, every student in a
        // freshly-opened term gets flagged as failing purely because nothing has been graded yet,
        // since a never-touched future position's score of 0 pulls the average straight to zero.
        // A genuinely-graded-then-closed-out cycle also ends at LOCKED, so this trades a small
        // amount of accuracy for a mid-term class (its most recent locked term's average won't
        // count here) against not flagging students on assessments nobody has touched yet - the
        // right call for a "needs attention right now" signal, unlike
        // ComputeEnrollmentYearAverageUseCase's year-end average, which deliberately does count
        // LOCKED because by then every cycle is expected to have actually been completed.
        @Query("""
                        SELECT
                            sa.enrollment.id,
                            AVG(a.score),
                            COUNT(a)
                        FROM AssessmentScoreEntity a
                        JOIN a.studentAssessment sa
                        JOIN a.cycle cy
                        WHERE a.schoolId = :schoolId
                          AND sa.enrollment.clazz.id = :classId
                          AND sa.term.id = :termId
                          AND cy.status NOT IN :excludedStatuses
                        GROUP BY sa.enrollment.id
                        """)
        List<Object[]> averageScoresByClassAndTerm(
                        @Param("schoolId") String schoolId,
                        @Param("classId") String classId,
                        @Param("termId") String termId,
                        @Param("excludedStatuses") List<ClassSubjectAssessmentLifeCycle.Status> excludedStatuses);

        // See AssessmentScoreRepository#studentSubjectAveragesForReport. classId nullable = whole
        // school for the term.
        @Query("""
                        SELECT sa.enrollment.id, sa.enrollment.student.id, sa.enrollment.student.givenNames,
                               sa.enrollment.student.familyName, sa.enrollment.clazz.id, sa.enrollment.clazz.name,
                               sa.teacherSubject.subject.id, sa.teacherSubject.subject.name, AVG(a.score), COUNT(a)
                        FROM AssessmentScoreEntity a
                        JOIN a.studentAssessment sa
                        JOIN a.cycle cy
                        WHERE a.schoolId = :schoolId
                          AND sa.term.id = :termId
                          AND (:classId IS NULL OR sa.enrollment.clazz.id = :classId)
                          AND (:subjectId IS NULL OR sa.teacherSubject.subject.id = :subjectId)
                          AND cy.status IN :statuses
                        GROUP BY sa.enrollment.id, sa.enrollment.student.id, sa.enrollment.student.givenNames,
                                 sa.enrollment.student.familyName, sa.enrollment.clazz.id, sa.enrollment.clazz.name,
                                 sa.teacherSubject.subject.id, sa.teacherSubject.subject.name
                        """)
        List<Object[]> studentSubjectAveragesForReport(
                        @Param("schoolId") String schoolId,
                        @Param("classId") String classId,
                        @Param("termId") String termId,
                        @Param("subjectId") String subjectId,
                        @Param("statuses") List<ClassSubjectAssessmentLifeCycle.Status> statuses);

        // See AssessmentScoreRepository#assessmentCompletionByClassAndTerm. classId nullable =
        // whole school for the term.
        @Query("""
                        SELECT sa.enrollment.id, COUNT(a),
                               SUM(CASE WHEN cy.status IN :approvedStatuses THEN 1L ELSE 0L END)
                        FROM AssessmentScoreEntity a
                        JOIN a.studentAssessment sa
                        JOIN a.cycle cy
                        WHERE a.schoolId = :schoolId
                          AND sa.term.id = :termId
                          AND (:classId IS NULL OR sa.enrollment.clazz.id = :classId)
                        GROUP BY sa.enrollment.id
                        """)
        List<Object[]> assessmentCompletionByClassAndTerm(
                        @Param("schoolId") String schoolId,
                        @Param("classId") String classId,
                        @Param("termId") String termId,
                        @Param("approvedStatuses") List<ClassSubjectAssessmentLifeCycle.Status> approvedStatuses);

        // See AssessmentScoreRepository#assessmentTrendByEnrollmentAndTerm.
        @Query("""
                        SELECT cy.assessment.id, cy.assessment.name, cy.assessment.position, AVG(a.score)
                        FROM AssessmentScoreEntity a
                        JOIN a.studentAssessment sa
                        JOIN a.cycle cy
                        WHERE a.schoolId = :schoolId
                          AND sa.enrollment.id = :enrollmentId
                          AND sa.term.id = :termId
                          AND cy.status IN :approvedStatuses
                        GROUP BY cy.assessment.id, cy.assessment.name, cy.assessment.position
                        ORDER BY cy.assessment.position
                        """)
        List<Object[]> assessmentTrendByEnrollmentAndTerm(
                        @Param("schoolId") String schoolId,
                        @Param("enrollmentId") String enrollmentId,
                        @Param("termId") String termId,
                        @Param("approvedStatuses") List<ClassSubjectAssessmentLifeCycle.Status> approvedStatuses);

        // See AssessmentScoreRepository#assessmentTrendByClassAndTerm. classId nullable = whole
        // school for the term (used by the attention report's declining-trend signal).
        @Query("""
                        SELECT sa.enrollment.id, cy.assessment.position, AVG(a.score)
                        FROM AssessmentScoreEntity a
                        JOIN a.studentAssessment sa
                        JOIN a.cycle cy
                        WHERE a.schoolId = :schoolId
                          AND sa.term.id = :termId
                          AND (:classId IS NULL OR sa.enrollment.clazz.id = :classId)
                          AND cy.status IN :approvedStatuses
                        GROUP BY sa.enrollment.id, cy.assessment.position
                        ORDER BY sa.enrollment.id, cy.assessment.position
                        """)
        List<Object[]> assessmentTrendByClassAndTerm(
                        @Param("schoolId") String schoolId,
                        @Param("classId") String classId,
                        @Param("termId") String termId,
                        @Param("approvedStatuses") List<ClassSubjectAssessmentLifeCycle.Status> approvedStatuses);

        // See AssessmentScoreRepository#assessmentAverageTrendForReport. classId/subjectId
        // nullable - the class/school-level counterpart to assessmentTrendByEnrollmentAndTerm
        // (which is per-student), for the Academic Trends section.
        @Query("""
                        SELECT cy.assessment.id, cy.assessment.name, cy.assessment.position, AVG(a.score)
                        FROM AssessmentScoreEntity a
                        JOIN a.studentAssessment sa
                        JOIN a.cycle cy
                        WHERE a.schoolId = :schoolId
                          AND sa.term.id = :termId
                          AND (:classId IS NULL OR sa.enrollment.clazz.id = :classId)
                          AND (:subjectId IS NULL OR sa.teacherSubject.subject.id = :subjectId)
                          AND cy.status IN :approvedStatuses
                        GROUP BY cy.assessment.id, cy.assessment.name, cy.assessment.position
                        ORDER BY cy.assessment.position
                        """)
        List<Object[]> assessmentAverageTrendForReport(
                        @Param("schoolId") String schoolId,
                        @Param("classId") String classId,
                        @Param("termId") String termId,
                        @Param("subjectId") String subjectId,
                        @Param("approvedStatuses") List<ClassSubjectAssessmentLifeCycle.Status> approvedStatuses);

        // See AssessmentScoreRepository#averageScoresForAttentionReport. Same shape/semantics as
        // averageScoresByClassAndTerm (used by the pre-existing ComputeClassAttentionUseCase), kept
        // as a separate method rather than widening that one so its contract for existing callers
        // never changes. classId nullable = whole school.
        @Query("""
                        SELECT
                            sa.enrollment.id,
                            AVG(a.score),
                            COUNT(a)
                        FROM AssessmentScoreEntity a
                        JOIN a.studentAssessment sa
                        JOIN a.cycle cy
                        WHERE a.schoolId = :schoolId
                          AND (:classId IS NULL OR sa.enrollment.clazz.id = :classId)
                          AND sa.term.id = :termId
                          AND cy.status NOT IN :excludedStatuses
                        GROUP BY sa.enrollment.id
                        """)
        List<Object[]> averageScoresForAttentionReport(
                        @Param("schoolId") String schoolId,
                        @Param("classId") String classId,
                        @Param("termId") String termId,
                        @Param("excludedStatuses") List<ClassSubjectAssessmentLifeCycle.Status> excludedStatuses);

        default Page<AssessmentScoreEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
                Specification<AssessmentScoreEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"),
                                schoolId);

                if (filters != null && !filters.isEmpty()) {
                        spec = spec.and(FilterSpecificationBuilder.build(filters));
                }

                return findAll(spec, pageable);
        }
}
