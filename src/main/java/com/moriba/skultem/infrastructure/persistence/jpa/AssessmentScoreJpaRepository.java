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
        // Excludes DRAFT cycles - a ClassSubjectAssessmentLifeCycle's score rows are scaffolded for
        // every student up front at score=0 before a teacher enters anything, and a term that just
        // started is mostly DRAFT cycles. Without this filter, every student in a freshly-opened
        // term gets flagged as failing purely because nothing has been graded yet. Same cutoff
        // AssessmentScoreAdapter#existsGradeActivityByClassIdAndSchoolId already uses to decide
        // whether a class has "real" grade activity.
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
                          AND cy.status <> :draftStatus
                        GROUP BY sa.enrollment.id
                        """)
        List<Object[]> averageScoresByClassAndTerm(
                        @Param("schoolId") String schoolId,
                        @Param("classId") String classId,
                        @Param("termId") String termId,
                        @Param("draftStatus") ClassSubjectAssessmentLifeCycle.Status draftStatus);

        default Page<AssessmentScoreEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
                Specification<AssessmentScoreEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"),
                                schoolId);

                if (filters != null && !filters.isEmpty()) {
                        spec = spec.and(FilterSpecificationBuilder.build(filters));
                }

                return findAll(spec, pageable);
        }
}
