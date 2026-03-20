package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;

public interface ClassSubjectAssessmentLifeCycleRepository {
    void save(ClassSubjectAssessmentLifeCycle domain);

    void saveAll(List<ClassSubjectAssessmentLifeCycle> domain);

    boolean existByTermAndAssessmentAndTeacherSubject(String termId, String assessmentId, String teacherSubjectId);

    List<ClassSubjectAssessmentLifeCycle> findAllBySubjectAndTerm(String subjectId, String tearmId);

    List<ClassSubjectAssessmentLifeCycle> findAllBySchoolAndTerm(String schoolId, String termId);

    List<ClassSubjectAssessmentLifeCycle> findAllBySchoolAndTermAndClass(String schoolId, String termId, String classId);

    List<ClassSubjectAssessmentLifeCycle> findAllBySchoolTermAndPosition(String schoolId, String termId, int position);

    Optional<ClassSubjectAssessmentLifeCycle> findByTeacherSubjectAndAssessmentAndTerm(String subectId,
            String assessmentId, String termId);
}
