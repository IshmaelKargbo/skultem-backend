package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.ClassMasterEntity;

@Repository
public interface ClassMasterJpaRepository extends JpaRepository<ClassMasterEntity, String> {
        boolean existsBySession_IdAndTeacher_IdAndSchoolId(String sessionId, String teacherId,
                        String schoolId);

        boolean existsBySession_IdAndSchoolId(String sessionId, String schoolId);

        Optional<ClassMasterEntity> findByIdAndSchoolId(String id, String schoolId);

        @EntityGraph(attributePaths = {
                        "teacher",
                        "teacher.user",
                        "session",
                        "session.stream",
                        "session.section"
        })
        Optional<ClassMasterEntity> findTopBySession_IdAndEndedAtIsNullOrderByAssignedAtDesc(String sessionId);

        Page<ClassMasterEntity> findAllBySchoolId(String schoolId, Pageable pageable);

        Page<ClassMasterEntity> findAllByTeacher_IdAndSession_AcademicYear_Id(String teacherId, String academicYearId, Pageable pageable);

        Page<ClassMasterEntity> findAllBySession_Id(String sessionId, Pageable pageable);

        Optional<ClassMasterEntity> findAllBySession_IdAndSchoolId(String sessionId, String schoolId);

        long countBySchoolId(String schoolId);
}
