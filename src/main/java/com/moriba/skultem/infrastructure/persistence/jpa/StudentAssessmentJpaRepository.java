package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.StudentAssessmentEntity;

public interface StudentAssessmentJpaRepository extends JpaRepository<StudentAssessmentEntity, String> {
    boolean existsByEnrollment_IdAndTeacherSubject_IdAndSchoolId(String enrollmentId, String subjectId, String schoolId);

    boolean existsByEnrollment_IdAndTerm_IdAndTeacherSubject_Subject_IdAndSchoolId(String enrollmentId, String termId,
            String subjectId, String schoolId);

    void deleteByEnrollment_IdAndTeacherSubject_IdAndSchoolId(String enrollmentId, String subjectId, String schoolId);

    List<StudentAssessmentEntity> findAllByTeacherSubject_Id(String teacherId);

    List<StudentAssessmentEntity> findAllByTeacherSubject_IdAndTerm_Id(String teacherId, String termId);

    // Resolves by the real subject + class session rather than one specific TeacherSubject row -
    // a subject can now have more than one teacher assigned, and they must all land on the same
    // shared set of student assessments rather than each seeing only the rows stamped with their
    // own teacher_subject_id (whichever teacher's assignment happened to trigger provisioning
    // first "owns" that stamp, see ProvisionStudentAssessmentsUseCase).
    @Query("""
                select sa from StudentAssessmentEntity sa
                where sa.teacherSubject.subject.id = :subjectId
                and sa.teacherSubject.session.id = :sessionId
                and sa.term.id = :termId
            """)
    List<StudentAssessmentEntity> findAllBySubjectAndSessionAndTermId(
            @Param("subjectId") String subjectId,
            @Param("sessionId") String sessionId,
            @Param("termId") String termId);
}
