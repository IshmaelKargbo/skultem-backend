package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.StudentAssessmentEntity;

@Repository
public interface StudentAssessmentJpaRepository extends JpaRepository<StudentAssessmentEntity, String> {
    boolean existsByEnrollment_IdAndTeacherSubject_IdAndSchoolId(String enrollmentId, String subjectId, String schoolId);

    boolean existsByEnrollment_IdAndTerm_IdAndTeacherSubject_Subject_IdAndSchoolId(String enrollmentId, String termId,
            String subjectId, String schoolId);

    void deleteByEnrollment_IdAndTeacherSubject_IdAndSchoolId(String enrollmentId, String subjectId, String schoolId);

    List<StudentAssessmentEntity> findAllByTeacherSubject_Id(String teacherId);

    List<StudentAssessmentEntity> findAllByTeacherSubject_IdAndTerm_Id(String teacherId, String termId);
}
