package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.infrastructure.persistence.entity.TimetableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TimetableJpaRepository extends JpaRepository<TimetableEntity, String> {
    boolean existsByPeriodIdAndDayIdAndSchoolId(String period, String day, String schoolId);

    List<TimetableEntity> findByPeriodIdAndDayIdAndSchoolIdOrderByCreatedAtAsc(String period, String day, String schoolId);

    Optional<TimetableEntity> findByIdAndSchoolId(String id, String schoolId);

    List<TimetableEntity> findByPeriodId(String periodId);

    // Every slot for one subject in one academic year - Library Attendance's scheduled sessions.
    @Query("""
                select t from TimetableEntity t
                where t.schoolId = :schoolId
                and t.teacherSubject.subject.id = :subjectId
                and t.period.session.academicYear.id = :academicYearId
            """)
    List<TimetableEntity> findBySubjectAndAcademicYear(@Param("subjectId") String subjectId,
            @Param("academicYearId") String academicYearId, @Param("schoolId") String schoolId);
}
