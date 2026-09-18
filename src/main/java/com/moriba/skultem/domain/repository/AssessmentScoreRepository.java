package com.moriba.skultem.domain.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.vo.Filter;

public interface AssessmentScoreRepository {
    void save(AssessmentScore domain);

    void saveAll(List<AssessmentScore> domain);

    boolean existByStudentAssessmentAndCycle(String studentAssessmentId, String assessmentId);

    List<AssessmentScore> findAllByStudentAssessment(String assessmentId);

    List<AssessmentScore> findAllByCycle(String cycleId);

    List<AssessmentScore> findAllByStudentAssessmentIdAndCycle(String studentAssessmentId, String cycleId);

    List<AssessmentScore> findAllByStudentAssessmentIdAndAssessmentId(String studentAssessmentId, String assessmentId);

    List<AssessmentScore> findAllByEnrollmentIdAndSchoolId(String enrollmentId, String schoolId);

    boolean existsGradeActivityByClassIdAndSchoolId(String classId, String schoolId);

    boolean existsGradeActivityByClassIdAndSubjectIdAndSchoolId(String classId, String subjectId, String schoolId);

    /**
     * Same as {@link #existsGradeActivityByClassIdAndSubjectIdAndSchoolId}, scoped to one academic
     * year - used to gate whether a class's subject curriculum can still be edited. Grade activity in
     * a past year must not freeze the subject out of every year that follows it.
     */
    boolean existsGradeActivityByClassIdAndSubjectIdAndAcademicYearIdAndSchoolId(String classId, String subjectId,
            String academicYearId, String schoolId);

    Page<AssessmentScore> runReport(String schoolId, List<Filter> filters, Pageable pageable);

    Integer getStudentRank(String schoolId, String classId, String termId, String studentId);

    // Row shape: [enrollmentId (String), averageScore (Double), scoreCount (Long)] - see
    // ComputeClassAttentionUseCase. excludedStatuses (DRAFT, LOCKED) are left out (see the JPA
    // query for why).
    List<Object[]> averageScoresByClassAndTerm(String schoolId, String classId, String termId,
            List<ClassSubjectAssessmentLifeCycle.Status> excludedStatuses);

    // Row shape: [enrollmentId, studentId, givenNames, familyName, classId, className, subjectId,
    // subjectName, averageScore (Double), scoreCount (Long)] - one row per student per subject,
    // restricted to APPROVED/COMPLETED cycles only (management reports show released data, unlike
    // the "needs attention" heuristic above which deliberately includes SUBMITTED/RETURNED).
    // classId/subjectId may both be null (whole school); className/classId let the caller also
    // group these same rows by class for a cross-class comparison, so Overview, Subject
    // Performance, Student Performance and the Class Performance table all agree with each other.
    List<Object[]> studentSubjectAveragesForReport(String schoolId, String classId, String termId,
            String subjectId, List<ClassSubjectAssessmentLifeCycle.Status> approvedStatuses);

    // Row shape: [enrollmentId, totalAssessments (Long), completedAssessments (Long)] - backs the
    // Student Performance table's "assessments completed / missing" columns. classId nullable =
    // whole school.
    List<Object[]> assessmentCompletionByClassAndTerm(String schoolId, String classId, String termId,
            List<ClassSubjectAssessmentLifeCycle.Status> approvedStatuses);

    // Row shape: [assessmentId, assessmentName, position, averageScore (Double)], ordered by
    // assessment position within the term - the chronological series behind the Performance Trends
    // drill-down for one student.
    List<Object[]> assessmentTrendByEnrollmentAndTerm(String schoolId, String enrollmentId, String termId,
            List<ClassSubjectAssessmentLifeCycle.Status> approvedStatuses);

    // Bulk, class/school-wide equivalent of assessmentTrendByEnrollmentAndTerm - one query for
    // every student's trend series instead of one query per student, for the "declining trend"
    // signal in Students Requiring Attention. Row shape: [enrollmentId, position, averageScore
    // (Double)], ordered by enrollment then position so grouping the rows in Java preserves
    // chronological order per student. classId nullable = whole school.
    List<Object[]> assessmentTrendByClassAndTerm(String schoolId, String classId, String termId,
            List<ClassSubjectAssessmentLifeCycle.Status> approvedStatuses);

    // Class/school-level (not per-student) chronological average per assessment position - the
    // Academic Trends section. Row shape: [assessmentId, assessmentName, position, averageScore
    // (Double)], ordered by position. classId/subjectId nullable.
    List<Object[]> assessmentAverageTrendForReport(String schoolId, String classId, String termId,
            String subjectId, List<ClassSubjectAssessmentLifeCycle.Status> approvedStatuses);

    // Same shape/semantics as averageScoresByClassAndTerm (deliberately kept separate so that
    // existing method's contract for ComputeClassAttentionUseCase never changes) but with classId
    // nullable, for the whole-school variant of Students Requiring Attention.
    List<Object[]> averageScoresForAttentionReport(String schoolId, String classId, String termId,
            List<ClassSubjectAssessmentLifeCycle.Status> excludedStatuses);
}
