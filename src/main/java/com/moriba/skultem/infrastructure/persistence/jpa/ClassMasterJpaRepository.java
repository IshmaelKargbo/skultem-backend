package com.moriba.skultem.infrastructure.persistence.jpa;

import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ClassMasterEntity;

public interface ClassMasterJpaRepository extends JpaRepository<ClassMasterEntity, String> {
        boolean existsBySession_IdAndTeacher_IdAndSchoolIdAndEndedAtIsNull(String sessionId, String teacherId,
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

        @EntityGraph(attributePaths = {
                        "teacher",
                        "teacher.user"
        })
        List<ClassMasterEntity> findAllBySession_IdAndSchoolIdAndEndedAtIsNull(String sessionId, String schoolId);

        long countBySchoolId(String schoolId);

        // Distinct teachers actively class-mastering a session in these levels this academic year -
        // the section-scoped counterpart of TeacherRepository#countAllBySchool for the Dashboard.
        @Query("""
                select count(distinct cm.teacher.id) from ClassMasterEntity cm
                where cm.schoolId = :schoolId and cm.endedAt is null
                and cm.session.academicYear.id = :academicYearId
                and cm.session.clazz.level in :levels
                """)
        long countDistinctTeachersInLevels(@Param("schoolId") String schoolId,
                        @Param("academicYearId") String academicYearId,
                        @Param("levels") java.util.Collection<com.moriba.skultem.domain.vo.Level> levels);
}
