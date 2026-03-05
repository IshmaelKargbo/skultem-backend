package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.TeacherSubjectEntity;

@Repository
public interface TeacherSubjectJpaRepository extends JpaRepository<TeacherSubjectEntity, String> {
    boolean existsByTeacher_IdAndSession_IdAndSubject_IdAndSchoolId(String teacherId, String sessionId, String subjectId, String schoolId);

    boolean existsBySession_IdAndSubject_IdAndSchoolId(String sessionId, String subjectId, String schoolId);

    Page<TeacherSubjectEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    Optional<TeacherSubjectEntity> findByIdAndSchoolId(String id, String schoolId);

    void deleteBySession_Clazz_IdAndSubject_IdAndSchoolId(String classId, String subjectId, String schoolId);

    void deleteBySession_Stream_IdAndSubject_IdAndSchoolId(String streamId, String subjectId, String schoolId);

    Page<TeacherSubjectEntity> findAllByTeacherId(String teacherId, Pageable pageable);

    Page<TeacherSubjectEntity> findAllBySubjectId(String subjectId, Pageable pageable);

    Page<TeacherSubjectEntity> findAllBySessionIdAndSchoolId(String sessionId, String schoolId, Pageable pageable);

    List<TeacherSubjectEntity> findByTeacher_IdAndSchoolId(String teacherId, String schoolId);

    Optional<TeacherSubjectEntity> findBySubject_IdAndSession_IdAndSchoolId(String subjectId, String sessionId, String schoolId);

    Optional<TeacherSubjectEntity> findOneByTeacher_IdAndSchoolId(String teacherId, String schoolId);

    Page<TeacherSubjectEntity> findAllByTeacher_IdAndSessionId(String teacherId, String sessionId, Pageable pageable);

    Page<TeacherSubjectEntity> findAllBySession_AcademicYear_IdAndSchoolId(String academicYear, String schoolId, Pageable pageable);
}
