package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.domain.model.Week;
import com.moriba.skultem.infrastructure.persistence.entity.SchemeOfWorkEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SchemeOfWorkJpaRepository extends JpaRepository<SchemeOfWorkEntity, String> {
    boolean existsByTermIdAndSubjectIdAndSessionIdAndSchoolId(String termId, String subjectId, String sessionId, String schoolId);

    Optional<SchemeOfWorkEntity> findByTermIdAndSubjectIdAndSessionIdAndSchoolId(String termId, String subjectId, String sessionId, String schoolId);

    Page<SchemeOfWorkEntity> findAllBySchoolId(String school, Pageable pageable);

    @Query("""
                SELECT s FROM SchemeOfWorkEntity s
                WHERE s.schoolId = :schoolId
                AND EXISTS (
                    SELECT 1 FROM TeacherSubjectEntity ts
                    WHERE ts.teacher.id = :teacherId
                    AND ts.subject.id = s.subject.id
                    AND ts.session.id = s.session.id
                )
            """)
    Page<SchemeOfWorkEntity> findAllByTeacherIdAndSchoolId(
            @Param("teacherId") String teacherId,
            @Param("schoolId") String schoolId,
            Pageable pageable);

    // The progress filter matches a scheme's rolled-up week progress (see Week.deriveProgress):
    // COMPLETED = has weeks and none are anything but COMPLETED; NOT_STARTED = no week has been
    // started (including schemes with no weeks yet); IN_PROGRESS = anything else.
    @Query("""
                SELECT s FROM SchemeOfWorkEntity s
                WHERE s.schoolId = :schoolId
                AND (:subjectId IS NULL OR s.subject.id = :subjectId)
                AND (:sessionId IS NULL OR s.session.id = :sessionId)
                AND (:termId IS NULL OR s.term.id = :termId)
                AND (
                    :progress IS NULL
                    OR (:progress = com.moriba.skultem.domain.model.Week.State.COMPLETED
                        AND EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id)
                        AND NOT EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week.State.COMPLETED))
                    OR (:progress = com.moriba.skultem.domain.model.Week.State.NOT_STARTED
                        AND NOT EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week.State.NOT_STARTED))
                    OR (:progress = com.moriba.skultem.domain.model.Week.State.IN_PROGRESS
                        AND EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week.State.NOT_STARTED)
                        AND EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week.State.COMPLETED))
                )
                ORDER BY s.createdAt DESC
            """)
    Page<SchemeOfWorkEntity> search(
            @Param("schoolId") String schoolId,
            @Param("subjectId") String subjectId,
            @Param("sessionId") String sessionId,
            @Param("termId") String termId,
            @Param("progress") Week.State progress,
            Pageable pageable);

    @Query("""
                SELECT s FROM SchemeOfWorkEntity s
                WHERE s.schoolId = :schoolId
                AND (:subjectId IS NULL OR s.subject.id = :subjectId)
                AND (:sessionId IS NULL OR s.session.id = :sessionId)
                AND (:termId IS NULL OR s.term.id = :termId)
                AND (
                    :progress IS NULL
                    OR (:progress = com.moriba.skultem.domain.model.Week.State.COMPLETED
                        AND EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id)
                        AND NOT EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week.State.COMPLETED))
                    OR (:progress = com.moriba.skultem.domain.model.Week.State.NOT_STARTED
                        AND NOT EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week.State.NOT_STARTED))
                    OR (:progress = com.moriba.skultem.domain.model.Week.State.IN_PROGRESS
                        AND EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week.State.NOT_STARTED)
                        AND EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week.State.COMPLETED))
                )
                AND EXISTS (
                    SELECT 1 FROM TeacherSubjectEntity ts
                    WHERE ts.teacher.id = :teacherId
                    AND ts.subject.id = s.subject.id
                    AND ts.session.id = s.session.id
                )
                ORDER BY s.createdAt DESC
            """)
    Page<SchemeOfWorkEntity> searchByTeacher(
            @Param("teacherId") String teacherId,
            @Param("schoolId") String schoolId,
            @Param("subjectId") String subjectId,
            @Param("sessionId") String sessionId,
            @Param("termId") String termId,
            @Param("progress") Week.State progress,
            Pageable pageable);
}
