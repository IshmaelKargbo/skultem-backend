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
    // ComputeClassAttentionUseCase. draftStatus is excluded (see the JPA query for why).
    List<Object[]> averageScoresByClassAndTerm(String schoolId, String classId, String termId,
            ClassSubjectAssessmentLifeCycle.Status draftStatus);
}
