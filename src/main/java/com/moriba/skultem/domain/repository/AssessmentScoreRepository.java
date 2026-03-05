package com.moriba.skultem.domain.repository;

import java.util.List;

import com.moriba.skultem.domain.model.AssessmentScore;

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
}
