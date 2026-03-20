package com.moriba.skultem.domain.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.vo.Filter;

public interface AssessmentScoreRepository {
    void save(AssessmentScore domain);

    void saveAll(List<AssessmentScore> domain);

    boolean existByStudentAssessmentAndCycle(String studentAssessmentId, String assessmentId);

    List<AssessmentScore> findAllByStudentAssessment(String assessmentId);

    List<AssessmentScore> findAllByCycle(String cycleId);

    List<AssessmentScore> findAllByStudentAssessmentIdAndCycle(String studentAssessmentId, String cycleId);

    List<AssessmentScore> findAllByStudentAssessmentIdAndAssessmentId(String studentAssessmentId, String assessmentId);

    boolean existsGradeActivityByClassIdAndSchoolId(String classId, String schoolId);

    boolean existsGradeActivityByClassIdAndSubjectIdAndSchoolId(String classId, String subjectId, String schoolId);

    Page<AssessmentScore> runReport(String schoolId, List<Filter> filters, Pageable pageable);

    Integer getStudentRank(String schoolId, String classId, String termId, String studentId);
}
