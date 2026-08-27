package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.infrastructure.persistence.entity.LessonEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonJpaRepository extends JpaRepository<LessonEntity, String> {
    boolean existsByWeekIdAndTitleAndSchoolId(String week, String title, String school);

    List<LessonEntity> findByWeekIdOrderByDateAsc(String week);

    @Query("""
                SELECT l FROM LessonEntity l
                WHERE l.schoolId = :schoolId
                AND EXISTS (
                    SELECT 1 FROM TeacherSubjectEntity ts
                    WHERE ts.teacher.id = :teacherId
                    AND ts.subject.id = l.week.scheme.subject.id
                    AND ts.session.id = l.week.scheme.session.id
                )
                ORDER BY l.date DESC
            """)
    Page<LessonEntity> findAllByTeacherIdAndSchoolId(
            @Param("teacherId") String teacherId,
            @Param("schoolId") String schoolId,
            Pageable pageable);
}
