package com.moriba.skultem.domain.repository;

import java.util.List;

import com.moriba.skultem.domain.model.StudentAssessment;

public interface StudentAssessmentRepository {
    void save(StudentAssessment domain);

    void saveAll(List<StudentAssessment> domain);

    boolean existsByEnrollmentIdAndAndSubjectIdAndSchoolId(String enrollmentId, String termId,
            String subjectId, String schoolId);

    boolean existsByEnrollmentIdAndTermIdAndSubjectIdAndSchoolId(String enrollmentId, String termId, String subjectId,
            String schoolId);

    void deleteByEnrollmentIdAndSubjectIdAndSchoolId(String enrollmentId, String subjectId, String schoolId);

    List<StudentAssessment> findAllByTeacherSubjectId(String teacherId);

    List<StudentAssessment> findAllByTeacherSubjectIdTermId(String teacherId, String termId);
}
